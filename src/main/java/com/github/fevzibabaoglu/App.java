package com.github.fevzibabaoglu;

import javax.swing.SwingUtilities;

import com.github.fevzibabaoglu.gui.MainFrame;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
