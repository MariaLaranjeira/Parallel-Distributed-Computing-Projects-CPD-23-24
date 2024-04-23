/*package org.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.json.*;

public class Registration {
    private static final String USER_FILE = "../database/users.json";

    public static void registerUser(String username, String password) throws IOException {
        JSONObject user = new JSONObject();
        user.put("username", username);
        user.put("password", hashPassword(password)); // Hash the password for security
        user.put("level", 1); // Default level

        JSONObject root;
        File file = new File(USER_FILE);
        if (file.exists()) {
            String content = new String(Files.readAllBytes(Paths.get(USER_FILE)));
            root = new JSONObject(content);
        } else {
            root = new JSONObject();
            root.put("users", new JSONArray());
        }
        root.getJSONArray("users").put(user);
        try (FileWriter writer = new FileWriter(USER_FILE)) {
            writer.write(root.toString(4)); // Write JSON file with indentation
        }
    }

    public static boolean authenticateUser(String username, String password) throws IOException {
        File file = new File(USER_FILE);
        if (file.exists()) {
            String content = new String(Files.readAllBytes(Paths.get(USER_FILE)));
            JSONObject root = new JSONObject(content);
            JSONArray users = root.getJSONArray("users");
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                if (user.getString("username").equals(username) &&
                        user.getString("password").equals(hashPassword(password))) {
                    return true; // User authenticated
                }
            }
        }
        return false;
    }

    private static String hashPassword(String password) {
        // Implement password hashing, e.g., using SHA-256
        return password; // Simplified for example
    }
}
 */