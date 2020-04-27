package com.unhuman.couchbaseui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
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

        for (N1QLQueryRefreshHandling n1QLQueryRefreshHandling : N1QLQueryRefreshHandling.values()) {
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(9, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Secret Key:");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        passwordSecretKey = new JPasswordField();
        panel4.add(passwordSecretKey, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Secret Match:");
        panel4.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        passwordSecretMatch = new JPasswordField();
        panel4.add(passwordSecretMatch, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Duplicate Query Handling:");
        panel4.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxDuplicateHandling = new JComboBox();
        panel4.add(comboBoxDuplicateHandling, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel4.add(spacer3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("CouchbaseUI Log Level:");
        panel4.add(label4, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Couchbase Client Log Level:");
        panel4.add(label5, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxCouchbaseUILogLevel = new JComboBox();
        panel4.add(comboBoxCouchbaseUILogLevel, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxCouchbaseClientLogLevel = new JComboBox();
        panel4.add(comboBoxCouchbaseClientLogLevel, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel4.add(spacer4, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Log History Size:");
        panel4.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldLogHistorySize = new JTextField();
        textFieldLogHistorySize.setName("");
        panel4.add(textFieldLogHistorySize, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
