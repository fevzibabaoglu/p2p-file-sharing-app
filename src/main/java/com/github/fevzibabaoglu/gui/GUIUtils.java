package com.github.fevzibabaoglu.gui;

import javax.swing.*;
import java.awt.Component;
import java.io.File;

public class GUIUtils {

    public static void showMessageDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static String getInputFromDialog(Component parent, String message) {
        return JOptionPane.showInputDialog(parent, message);
    }

    public static String getFolderFromDialog(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Set the default directory to the workspace folder
        String workspacePath = System.getProperty("user.dir");
        chooser.setCurrentDirectory(new File(workspacePath));

        int result = chooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            return folder.getAbsolutePath();
        }
        return null;
    }
}
