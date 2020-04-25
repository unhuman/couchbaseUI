package com.unhuman.couchbaseui;

import com.unhuman.couchbaseui.config.CouchbaseUIConfig;
import com.unhuman.couchbaseui.config.N1QLQueryRefreshHandling;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;

import static com.codingrodent.jackson.crypto.PasswordCryptoContext.MIN_PASSWORD_LENGTH;
import static com.unhuman.couchbaseui.config.CouchbaseUIConfig.MAXIMUM_LOG_SIZE;

public class SettingsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPasswordField passwordSecretKey;
    private JPasswordField passwordSecretMatch;
    private JComboBox comboBoxDuplicateHandling;
    private JComboBox comboBoxCouchbaseUILogLevel;
    private JComboBox comboBoxCouchbaseClientLogLevel;
    private JTextField textFieldLogHistorySize;

    public SettingsDialog(CouchbaseUIConfig configuration) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        if (configuration.hasSecret()) {
            passwordSecretKey.setText(configuration.getSecret());
            passwordSecretMatch.setText(configuration.getSecret());
        }

        for (N1QLQueryRefreshHandling n1QLQueryRefreshHandling: N1QLQueryRefreshHandling.values()) {
            comboBoxDuplicateHandling.addItem(n1QLQueryRefreshHandling);
        }
        comboBoxDuplicateHandling.setSelectedItem(configuration.getN1QLQueryRefreshHandling());

        comboBoxCouchbaseUILogLevel.addItem(Level.OFF);
        comboBoxCouchbaseUILogLevel.addItem(Level.SEVERE);
        comboBoxCouchbaseUILogLevel.addItem(Level.WARNING);
        comboBoxCouchbaseUILogLevel.addItem(Level.INFO);
        comboBoxCouchbaseUILogLevel.addItem(Level.FINE);
        comboBoxCouchbaseUILogLevel.addItem(Level.FINER);
        comboBoxCouchbaseUILogLevel.addItem(Level.FINEST);
        comboBoxCouchbaseUILogLevel.addItem(Level.ALL);
        comboBoxCouchbaseUILogLevel.setSelectedItem(configuration.getCouchbaseUILogLevel());

        comboBoxCouchbaseClientLogLevel.addItem(Level.OFF);
        comboBoxCouchbaseClientLogLevel.addItem(Level.SEVERE);
        comboBoxCouchbaseClientLogLevel.addItem(Level.WARNING);
        comboBoxCouchbaseClientLogLevel.addItem(Level.INFO);
        comboBoxCouchbaseClientLogLevel.addItem(Level.FINE);
        comboBoxCouchbaseClientLogLevel.addItem(Level.FINER);
        comboBoxCouchbaseClientLogLevel.addItem(Level.FINEST);
        comboBoxCouchbaseClientLogLevel.addItem(Level.ALL);
        comboBoxCouchbaseClientLogLevel.setSelectedItem(configuration.getCouchbaseClientLogLevel());

        textFieldLogHistorySize.setText(configuration.getLogHistorySize().toString());

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String key = new String(passwordSecretKey.getPassword()).trim();
                String match = new String(passwordSecretMatch.getPassword()).trim();

                // empty key + match = remove password
                if (key.length() == 0 && match.length() == 0) {
                    key = null;
                } else if (key.length() < MIN_PASSWORD_LENGTH) {
                    JOptionPane.showMessageDialog(contentPane,
                            "Secret Key is too short (" + MIN_PASSWORD_LENGTH + " required.)",
                            "Settings Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (!key.equals(match)) {
                    JOptionPane.showMessageDialog(contentPane, "Password Match is not the same as Password Key.",
                            "Settings Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // validate the log size
                int checkLogHistorySize;
                try {
                    checkLogHistorySize = Integer.parseInt(textFieldLogHistorySize.getText());

                    if (checkLogHistorySize < 0) {
                        JOptionPane.showMessageDialog(contentPane, "Log History Size must be > 0.",
                                "Settings Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (checkLogHistorySize > MAXIMUM_LOG_SIZE) {
                        JOptionPane.showMessageDialog(contentPane,
                                "Log History Size must be <= " + MAXIMUM_LOG_SIZE + ".",
                                "Settings Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(contentPane, "Log History Size must be a valid integer.",
                            "Settings Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                configuration.setLogHistorySize(checkLogHistorySize);
                configuration.setCouchbaseUILogLevel((Level) comboBoxCouchbaseUILogLevel.getSelectedItem());
                configuration.setCouchbaseClientLogLevel((Level) comboBoxCouchbaseClientLogLevel.getSelectedItem());

                configuration.setSecret(key);
                configuration.setN1QLQueryRefreshHandling(
                        (N1QLQueryRefreshHandling) comboBoxDuplicateHandling.getSelectedItem());

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
