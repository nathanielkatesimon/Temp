/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

/**
 *
 * @author Angel Marie Eltagonde
 */

public class TransparentWhitePanel extends JPanel {
    private static final float ALPHA = 0.5f; // Transparency level (20%)

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Set semi-transparent white background
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ALPHA));
        g2d.setColor(Color.WHITE); // White color
        g2d.fillRect(0, 0, getWidth(), getHeight()); // Fill the panel

        g2d.dispose();
    }
}
