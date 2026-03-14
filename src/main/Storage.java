package main;

import java.util.concurrent.ConcurrentHashMap;

public final class Storage {
	private static ConcurrentHashMap<String,ClientHandler> clients = new ConcurrentHashMap<>();
	
	public static ConcurrentHashMap<String, ClientHandler> getClients() {
		return clients;
	}
}
