package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import packet.CustomPacket;
import packet.CustomPacketType;

public class MyOwnClient {

    public static void main(String[] args) {

        try {

            Socket socket = new Socket("localhost", 5000);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);

            System.out.print("Username: ");
            String username = scanner.nextLine();

            CustomPacket loginPacket = new CustomPacket(
                    username,
                    "SERVER",
                    CustomPacketType.LOGIN,
                    "hello"
            );

            out.println(loginPacket.serializePacket());

            new Thread(() -> {

                try {

                    String raw;

                    while ((raw = in.readLine()) != null) {

                        CustomPacket packet = CustomPacket.parser(raw);

                        switch (packet.getType()) {

                            case RECEIVE_MESSAGE:
                                System.out.println(
                                        packet.getFrom() + ": " + packet.getMessage()
                                );
                                break;

                            case INFO:
                                System.out.println("[INFO] " + packet.getMessage());
                                break;

                            case ERROR:
                                System.out.println("[ERROR] " + packet.getMessage());
                                break;

                            default:
                                System.out.println(packet.getMessage());
                        }
                    }

                } catch (Exception e) {

                    System.out.println("Server disconnected");
                }

            }).start();

            // Kullanıcı input loop
            while (true) {

                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("/exit")) {

                    CustomPacket logout = new CustomPacket(
                            username,
                            "SERVER",
                            CustomPacketType.LOGOUT,
                            "bye"
                    );

                    out.println(logout.serializePacket());
                    socket.close();
                    break;
                }

                // format: kullanıcı:mesaj
                if (input.contains(":")) {

                    String[] parts = input.split(":", 2);

                    String target = parts[0];
                    String message = parts[1];

                    CustomPacket messagePacket = new CustomPacket(
                            username,
                            target,
                            CustomPacketType.SEND_MESSAGE,
                            message
                    );

                    out.println(messagePacket.serializePacket());

                } else {

                    System.out.println("Format: kullanıcı:mesaj");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
