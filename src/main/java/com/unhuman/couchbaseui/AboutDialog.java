package com.unhuman.couchbaseui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AboutDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;

    public AboutDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    public static void display(Component component) {
        AboutDialog dialog = new AboutDialog();
        dialog.setTitle("About CouchbaseUI");
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(component);
        dialog.setVisible(true);
    }
}
