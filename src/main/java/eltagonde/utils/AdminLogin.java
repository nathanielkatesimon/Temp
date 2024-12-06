/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.swing.*;

public class AdminLogin {

    // Method to authenticate admin login
    public static boolean authenticate(String username, String password) {
        String filePath = "config.txt";
        try {
            // Read the file containing admin credentials
            String credentials = new String(Files.readAllBytes(Paths.get(filePath)));
            
            // Split by ':'
            String[] parts = credentials.split(":");
            if (parts.length != 2) {
                JOptionPane.showMessageDialog(null, "Invalid configuration format.");
                return false;
            }

            String storedUsername = parts[0];
            String storedHashedPassword = parts[1];

            // Check if username matches and password is correct
            if (storedUsername.equals(username) && PasswordUtil.hashPassword(password).equals(storedHashedPassword)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static void updateAdminCredentials(String newUsername, String newPassword) {
        // Hash the password
        String hashedPassword = PasswordUtil.hashPassword(newPassword);

        // Write to the config file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("config.txt"))) {
            writer.write(newUsername + ":" + hashedPassword);
            System.out.println("Admin credentials set successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

