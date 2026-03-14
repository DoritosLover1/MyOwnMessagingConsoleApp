package main;

import java.io.*;
import java.net.Socket;
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
                            "SERVER",
                            "UNKNOWN",
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
                        "SERVER",
                        "ALL",
                        CustomPacketType.INFO,
                        username + " left chat"
                ));
            }

            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    private void handlePacket(CustomPacket packet) {

        if (username == null && packet.getType() != CustomPacketType.LOGIN) {

            sendPacket(new CustomPacket(
                    "SERVER",
                    packet.getFrom(),
                    CustomPacketType.ERROR,
                    "You must LOGIN first"
            ));

            return;
        }

        switch (packet.getType()) {

            case LOGIN:

                if (username != null) {

                    sendPacket(new CustomPacket(
                            "SERVER",
                            packet.getFrom(),
                            CustomPacketType.ERROR,
                            "Already logged in"
                    ));
                    return;
                }

                if (Storage.getClients().containsKey(packet.getFrom())) {

                    sendPacket(new CustomPacket(
                            "SERVER",
                            packet.getFrom(),
                            CustomPacketType.ERROR,
                            "Username already taken"
                    ));
                    return;
                }

                username = packet.getFrom();

                Storage.getClients().put(username, this);

                System.out.println(username + " connected");

                broadcast(new CustomPacket(
                        "SERVER",
                        "ALL",
                        CustomPacketType.INFO,
                        username + " joined chat"
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

                broadcast(new CustomPacket(
                        "SERVER",
                        "ALL",
                        CustomPacketType.INFO,
                        username + " logged out"
                ));

                username = null;

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

            client.sendPacket(packet);
        }
    }
}