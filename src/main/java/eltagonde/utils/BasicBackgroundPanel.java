package eltagonde.utils;

import java.awt.*;
import javax.swing.*;

public class BasicBackgroundPanel extends JPanel {
    private Image background;

    // Default constructor (required by GUI Builder)
    public BasicBackgroundPanel() {
        this(null); // Call the other constructor with a null image
    }

    // Constructor with background image
    public BasicBackgroundPanel(Image background) {
        this.background = background;
        setLayout(new BorderLayout());
    }

    // Setter for the background image
    public void setBackgroundImage(Image background) {
        this.background = background;
        repaint(); // Repaint the component to show the new background
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null); // Scaled image
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (background != null) {
            return new Dimension(background.getWidth(this), background.getHeight(this));
        }
        // Provide a default size if no background is set
        return new Dimension(200, 200); // Default size for GUI Builder
    }
}
