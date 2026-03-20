package main;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public final class Storage {

    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ArrayList<String>> friends = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, ClientHandler> getClients() {
        return clients;
    }

    public static void addFriend(String user, String friend) {
        friends.computeIfAbsent(user, k -> new ArrayList<>());
        if (!friends.get(user).contains(friend)) {
            friends.get(user).add(friend);
        }
    }

    public static ArrayList<String> getFriends(String user) {
    	return friends.computeIfAbsent(user, k -> new ArrayList<>());
    }
    
	public static boolean isNicknameTaken(String nickname) {
		return clients.containsKey(nickname);
	}
}