package com.unhuman.couchbaseui;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AlertsDialog extends JDialog {
    private boolean clearAlerts = false;
    private JPanel contentPane;
    private JButton buttonCancel;
    private JButton buttonClearAlerts;
    private JTable tableAlerts;

    public AlertsDialog(JSONArray alerts) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonCancel);

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        buttonClearAlerts.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearAlerts = true;
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

        tableAlerts.setModel(new AlertTableModel(alerts));
    }

    private void onCancel() {
        dispose();
    }

    public static boolean display(Component component, JSONArray alerts) {
        // Don't display if there's nothing here
        if (alerts == null || alerts.length() == 0) {
            return false;
        }

        AlertsDialog dialog = new AlertsDialog(alerts);
        dialog.setTitle("Couchbase Alerts");
        dialog.pack();
        dialog.setLocationRelativeTo(component);
        dialog.setVisible(true);

        return dialog.clearAlerts;
    }

    class AlertTableModel extends AbstractTableModel {
        final String[] columnNames = {"Time", "Message"};

        private String[][] data;

        public AlertTableModel(JSONArray alertsJson) {

            data = new String[alertsJson.length()][2];
            for (int i = 0; i < alertsJson.length(); i++) {
                try {
                    data[i][0] = ((JSONObject) alertsJson.get(i)).get("serverTime").toString();
                    data[i][1] = ((JSONObject) alertsJson.get(i)).get("msg").toString();
                } catch (JSONException je) {
                    // ignore for now;
                }
            }
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
    }

}
