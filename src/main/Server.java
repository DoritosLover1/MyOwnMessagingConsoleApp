package main;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
	public static void main(String[] args) throws IOException {
		
		ServerSocket serverSocket = new ServerSocket(5000);
		System.out.println("Server is running right now");
		
		while(true) {
			Socket socket = serverSocket.accept();
			System.out.println("New client's joined: " + socket);
			new ClientHandler(socket).start();
		}
	}
}
