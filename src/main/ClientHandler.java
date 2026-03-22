package main;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import packet.CustomPacket;
import packet.CustomPacketType;

public class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String raw;
            while ((raw = in.readLine()) != null) {
                try {
                    CustomPacket packet = CustomPacket.parser(raw);
                    handlePacket(packet);
                } catch (Exception e) {
                    sendPacket(new CustomPacket(
                            "SERVER", "UNKNOWN",
                            CustomPacketType.ERROR,
                            "Invalid packet format"
                    ));
                }
            }
        } catch (Exception e) {
            System.out.println("Connection closed: " + username);
        } finally {
            if (username != null) {
                Storage.getClients().remove(username);
                broadcast(new CustomPacket(
                        "SERVER", "ALL",
                        CustomPacketType.INFO,
                        username + " left chat"
                ));
            }
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void handlePacket(CustomPacket packet) {

        if (username == null && packet.getType() != CustomPacketType.LOGIN) {
            sendPacket(new CustomPacket(
                    "SERVER", packet.getFrom(),
                    CustomPacketType.ERROR,
                    "You must LOGIN first"
            ));
            return;
        }

        switch (packet.getType()) {

	        case LOGIN:
	            String requestedName = packet.getFrom();
	            
	            if (Storage.isNicknameTaken(requestedName)) {
	            	System.out.println("Username taken, sending error to: " + requestedName);
	                sendPacket(new CustomPacket("SERVER", requestedName, CustomPacketType.LOGIN_USERNAME_ERROR,
	                        "Username already taken"));
	                try { socket.close(); } catch (Exception ignored) {}
	                return;
	            }
	            
	            username = requestedName;
	            Storage.getClients().put(username, this);
	            System.out.println(username + " connected");
	            
	            sendPacket(new CustomPacket(
	                    "SERVER", username,
	                    CustomPacketType.INFO,
	                    "Login successful"
	            ));
	            break;

            case SEND_MESSAGE:

                if (packet.getTo().equalsIgnoreCase("ALL")) {

                    broadcast(new CustomPacket(
                            packet.getFrom(),
                            "ALL",
                            CustomPacketType.RECEIVE_MESSAGE,
                            packet.getMessage()
                    ));

                } else {

                    ClientHandler target = Storage.getClients().get(packet.getTo());

                    if (target != null) {

                        target.sendPacket(new CustomPacket(
                                packet.getFrom(),
                                packet.getTo(),
                                CustomPacketType.RECEIVE_MESSAGE,
                                packet.getMessage()
                        ));

                    } else {

                        sendPacket(new CustomPacket(
                                "SERVER",
                                username,
                                CustomPacketType.ERROR,
                                "User not found"
                        ));
                    }
                }

                break;

            case LOGOUT:
                Storage.getClients().remove(username);
                Storage.getFriends(username).forEach(friend -> {
                    ClientHandler friendTarget = Storage.getClients().get(friend);
                    if (friendTarget != null) {
                        List<String> updatedList = Storage.getFriends(friend)
                            .stream()
                            .filter(f -> !f.equals(username))
                            .collect(Collectors.toList());

                        friendTarget.sendPacket(new CustomPacket(
                            "SERVER",
                            friendTarget.username,
                            CustomPacketType.REFRESH_FRIENDS_LIST,
                            String.join(",", updatedList)
                        ));
                    }
                });
                try { socket.close(); } catch (Exception ignored) {}
                break;

            case ADD_FRIEND:
                String friendName = packet.getMessage();

                if (!Storage.getClients().containsKey(friendName)) {
                    sendPacket(new CustomPacket(
                            "SERVER", username,
                            CustomPacketType.ERROR,
                            "User not found"
                    ));
                    return;
                }

                Storage.addFriend(username, friendName);
                Storage.addFriend(friendName, username);

                String senderList = String.join(",", Storage.getFriends(username));
                sendPacket(new CustomPacket(
                        "SERVER", username,
                        CustomPacketType.USER_LIST,
                        senderList
                ));

                ClientHandler friendTarget = Storage.getClients().get(friendName);
                if (friendTarget != null) {
                    String targetList = String.join(",", Storage.getFriends(friendName));
                    friendTarget.sendPacket(new CustomPacket(
                            "SERVER", friendName,
                            CustomPacketType.REFRESH_FRIENDS_LIST,
                            targetList
                    ));
                }

                break;

            default:

                sendPacket(new CustomPacket(
                        "SERVER",
                        username,
                        CustomPacketType.ERROR,
                        "Unknown packet type"
                ));
        }
    }

    public void sendPacket(CustomPacket packet) {
        out.println(packet.serializePacket());
    }

    private void broadcast(CustomPacket packet) {
        for (ClientHandler client : Storage.getClients().values()) {
            if (client.username != null) {
                client.sendPacket(packet);
            }
        }
    }
}