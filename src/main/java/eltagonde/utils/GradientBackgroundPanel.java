/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.utils;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

/**
 *
 * @author Angel Marie Eltagonde
 */
public class GradientBackgroundPanel extends JPanel {

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Define the colors for the gradient
        Color color1 = new Color(4, 34, 90);   // #04225a
        Color color2 = new Color(19, 85, 99);  // #135563

        // Create a horizontal gradient from left (color1) to right (color2)
        GradientPaint gradient = new GradientPaint(0, 0, color1, getWidth(), 0, color2);

        // Set the gradient as the paint for the graphics
        g2d.setPaint(gradient);

        // Fill the entire panel with the gradient
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.dispose();
    }
}