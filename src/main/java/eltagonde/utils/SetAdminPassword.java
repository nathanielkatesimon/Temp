/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.utils;
import java.io.*;

public class SetAdminPassword {

    // Main method to set admin credentials
    public static void main(String[] args) {
        String username = "admin";  // Default admin username
        String password = "password";  // Default password, change this before running

        // Hash the password
        String hashedPassword = PasswordUtil.hashPassword(password);

        // Write to the config file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("config.txt"))) {
            writer.write(username + ":" + hashedPassword);
            System.out.println("Admin credentials set successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
