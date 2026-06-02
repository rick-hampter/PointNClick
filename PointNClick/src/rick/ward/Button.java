// this was all ai. going to delete.



package rick.ward;

import java.awt.*;
import java.awt.event.*;

public class Button {

    // -------------------------------------------------------------------------
    // Config
    // -------------------------------------------------------------------------
    private static final Color COL_NORMAL   = new Color(225, 225, 225);
    private static final Color COL_HOVER    = new Color(190, 230, 255);
    private static final Color COL_PRESSED  = new Color(100, 180, 240);
    private static final Color COL_BORDER   = new Color(100, 100, 100);
    private static final Color COL_TEXT     = Color.BLACK;
    private static final Font  FONT         = new Font("Segoe UI", Font.PLAIN, 13);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private final int x, y, width, height;
    private final String label;
    private final Runnable onClick;

    private boolean hovered = false;
    private boolean pressed = false;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    /**
     * @param x       left edge of the button
     * @param y       top edge of the button
     * @param width   button width
     * @param height  button height
     * @param label   text shown on the button
     * @param onClick runnable called when the button is clicked
     */
    public Button(int x, int y, int width, int height, String label, Runnable onClick) {
        this.x       = x;
        this.y       = y;
        this.width   = width;
        this.height  = height;
        this.label   = label;
        this.onClick = onClick;
    }

    // -------------------------------------------------------------------------
    // Call these from your GameWindow
    // -------------------------------------------------------------------------

    /**
     * Call from update() — pass in current mouse position and button state.
     *
     * @param mouseX      current mouse X
     * @param mouseY      current mouse Y
     * @param mouseDown   true if the left mouse button is currently held
     */
    public void update(int mouseX, int mouseY, boolean mouseDown) {
        hovered = contains(mouseX, mouseY);
        pressed = hovered && mouseDown;
    }

    /**
     * Call from your MouseListener's mouseReleased() to fire the click action.
     *
     * @param mouseX  X position of the release
     * @param mouseY  Y position of the release
     */
    public void handleRelease(int mouseX, int mouseY) {
        if (contains(mouseX, mouseY)) {
            onClick.run();
        }
    }

    /**
     * Call from draw() to render the button.
     */
    public void draw(Graphics2D g) {
        // Background
        if (pressed)       g.setColor(COL_PRESSED);
        else if (hovered)  g.setColor(COL_HOVER);
        else               g.setColor(COL_NORMAL);
        g.fillRoundRect(x, y, width, height, 6, 6);

        // Border
        g.setColor(COL_BORDER);
        g.drawRoundRect(x, y, width, height, 6, 6);

        // Label — centred
        g.setColor(COL_TEXT);
        g.setFont(FONT);
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (width  - fm.stringWidth(label)) / 2;
        int ty = y + (height - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(label, tx, ty);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private boolean contains(int px, int py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}