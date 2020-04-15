package com.unhuman.couchbaseui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.codingrodent.jackson.crypto.PasswordCryptoContext.MIN_PASSWORD_LENGTH;

public class SecretDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPasswordField passwordSecretKey;
    private JCheckBox checkBoxAllowCancel;

    private AtomicReference<String> secretKeyContainer;

    public SecretDialog(AtomicReference<String> secretKeyContainer) {
        this.checkBoxAllowCancel.setSelected(false);
        this.secretKeyContainer = secretKeyContainer;
        this.secretKeyContainer.set(null);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        String currentPassword = new String(passwordSecretKey.getPassword()).trim();
        if (currentPassword.length() < 8) {
            JOptionPane.showMessageDialog(contentPane,
                    "Secret is too short (" + MIN_PASSWORD_LENGTH + " required).",
                    "Secret Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        secretKeyContainer.set(currentPassword);
        dispose();
    }

    private void onCancel() {
        // Ensure user has approved cancel
        if (!checkBoxAllowCancel.isSelected()) {
            return;
        }
        secretKeyContainer.set(null);
        dispose();
    }

    public static void display(Component component, AtomicReference<String> passwordContainer) {
        SecretDialog dialog = new SecretDialog(passwordContainer);
        dialog.setTitle("CouchbaseUI Secret Request");
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(component);
        dialog.setVisible(true);
    }
}
