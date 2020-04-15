package com.unhuman.couchbaseui;

import com.unhuman.couchbaseui.config.CouchbaseUIConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static com.codingrodent.jackson.crypto.PasswordCryptoContext.MIN_PASSWORD_LENGTH;

public class SettingsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPasswordField passwordSecretKey;
    private JPasswordField passwordSecretMatch;

    public SettingsDialog(CouchbaseUIConfig configuration) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        if (configuration.hasSecret()) {
            passwordSecretKey.setText(configuration.getSecret());
            passwordSecretMatch.setText(configuration.getSecret());
        }

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String key = new String(passwordSecretKey.getPassword()).trim();
                String match = new String(passwordSecretMatch.getPassword()).trim();

                // empty key + match = remove password
                if (key.length() == 0 && match.length() == 0) {
                    key = null;
                } else if (key.length() < MIN_PASSWORD_LENGTH) {
                    JOptionPane.showMessageDialog(contentPane,
                            "Secret Key is too short (" + MIN_PASSWORD_LENGTH + " required)",
                            "Settings Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (!key.equals(match)) {
                    JOptionPane.showMessageDialog(contentPane, "Password Match is not the same as Password Key",
                            "Settings Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                configuration.setSecret(key);

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
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void display(Component component, CouchbaseUIConfig configuration) {
        SettingsDialog dialog = new SettingsDialog(configuration);
        dialog.setTitle("CouchbaseUI Settings");
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(component);
        dialog.setVisible(true);
    }
}
