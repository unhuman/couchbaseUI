package com.unhuman.couchbaseui;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.util.DefaultIndenter;
import com.couchbase.client.core.deps.com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.JsonNode;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectWriter;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.SerializationFeature;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JacksonTransformers;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.kv.RemoveOptions;
import com.couchbase.client.java.kv.ReplaceOptions;
import com.couchbase.client.java.query.QueryResult;
import com.unhuman.couchbaseui.config.ClusterConfig;
import com.unhuman.couchbaseui.config.ConfigFileManager;
import com.unhuman.couchbaseui.config.CouchbaseUIConfig;
import com.unhuman.couchbaseui.config.UserConfig;
import com.unhuman.couchbaseui.couchbase.CouchbaseClientManager;
import com.unhuman.couchbaseui.entities.BucketCollection;
import com.unhuman.couchbaseui.entities.ClusterConnection;
import com.unhuman.couchbaseui.utils.Utilities;
import us.monoid.json.JSONException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;

import static com.unhuman.couchbaseui.utils.Utilities.trimString;

public class CouchbaseUI {
    private static final Color DARK_RED = new Color(128, 0, 0);

    private final CouchbaseClientManager couchbase;
    private final ObjectWriter prettyPrintingWriter;
    private CouchbaseUIConfig config;

    private JPanel panel;
    private JComboBox comboClusterPicker;
    private JTabbedPane tabPanelOperations;
    private JPanel tabN1QL;
    private JPanel tabKeyValue;
    private JPanel panelN1QL;
    private JPanel panelStatus;
    private JTextField textStatus;
    private JButton fetchButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton createButton;
    private JTextField textfieldDocumentKey;
    private JTextArea textareaValue;
    private JPanel panelKVActions;
    private JTextField textfieldMetadata;
    private JPanel panelKVContent;
    private JPanel panelCluster;
    private JPanel panelMetadata;
    private JPasswordField password;
    private JComboBox comboboxUser;
    private JComboBox comboBucketName;
    private JTextArea textareaQueryResponse;
    private JTextArea textareaQuery;
    private JButton buttonQuery;
    private JComboBox comboboxCollection;

    private final Color textStatusDisabledTextColor;
    private final Color textStatusBgColor;

    private long current_cas = -1L;

    public CouchbaseUI() {
        // Set the global mapper to pretty print
        JacksonTransformers.MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        prettyPrintingWriter = JacksonTransformers.MAPPER.writer(prettyPrinter);

        textStatusDisabledTextColor = textStatus.getDisabledTextColor();
        textStatusBgColor = textStatus.getBackground();

        couchbase = new CouchbaseClientManager();

        /** Set up all the listeners */

        // Setup KV Create Document button
        createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Collection collection = getBucketCollection();

                    if (Utilities.stringIsNullOrEmpty(textfieldDocumentKey.getText())) {
                        throw new RuntimeException("Key must be provided");
                    }

                    if (Utilities.stringIsNullOrEmpty(textareaValue.getText())) {
                        throw new RuntimeException("Content must be provided");
                    }

                    updateStatusTextProcessing();

                    String documentKey = trimString(textfieldDocumentKey.getText());
                    // TODO: add TTL
                    JsonNode convertedObject = JacksonTransformers.MAPPER. readTree(textareaValue.getText());
                    MutationResult result = collection.insert(documentKey, convertedObject);
                    current_cas = result.cas();

                    updateStatusText("Create complete.");
                } catch (Exception e) {
                    updateStatusText(e);
                }
            }
        });
        // Setup KV Fetch Document button
        fetchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Collection collection = getBucketCollection();

                    if (Utilities.stringIsNullOrEmpty(textfieldDocumentKey.getText())) {
                        throw new RuntimeException("Key must be provided");
                    }

                    updateStatusTextProcessing();

                    String documentKey = trimString(textfieldDocumentKey.getText());
                    GetResult result = collection.get(documentKey);
                    textfieldMetadata.setText("CAS: " + result.cas() + " Expiry: " + result.expiry());

                    JsonObject resultContent = result.contentAsObject();
                    String prettyJson = prettyPrintingWriter.writeValueAsString(resultContent);

                    textareaValue.setText(prettyJson);
                    current_cas = result.cas();

                    updateStatusText("Fetch complete.");
                } catch (Exception e) {
                    updateStatusText(e);
                }
            }
        });
        // Setup KV Delete Document button
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Collection collection = getBucketCollection();

                    if (Utilities.stringIsNullOrEmpty(textfieldDocumentKey.getText())) {
                        throw new RuntimeException("Key must be provided");
                    }

                    updateStatusTextProcessing();

                    String documentKey = trimString(textfieldDocumentKey.getText());
                    RemoveOptions removeOptions = RemoveOptions.removeOptions()
                            .cas(current_cas);
                    collection.remove(documentKey, removeOptions);

                    updateStatusText("Delete complete.");
                } catch (DocumentNotFoundException dnfe) {
                    updateStatusText(dnfe);
                } catch (Exception e) {
                    updateStatusText(e);
                }
            }
        });

        // Setup KV Update Document button
        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Collection collection = getBucketCollection();

                    if (Utilities.stringIsNullOrEmpty(textfieldDocumentKey.getText())) {
                        throw new RuntimeException("Key must be provided");
                    }

                    if (Utilities.stringIsNullOrEmpty(textareaValue.getText())) {
                        throw new RuntimeException("Content must be provided");
                    }

                    updateStatusTextProcessing();

                    String documentKey = trimString(textfieldDocumentKey.getText());
                    // TODO: add TTL
                    ReplaceOptions replaceOptions = ReplaceOptions.replaceOptions()
                            .cas(current_cas);

                    JsonNode convertedObject = JacksonTransformers.MAPPER. readTree(textareaValue.getText());

                    MutationResult result = collection.replace(documentKey, convertedObject);
                    current_cas = result.cas();

                    updateStatusText("Update complete.");
                } catch (Exception e) {
                    updateStatusText(e);
                }
            }
        });

        // Setup N1QL Query button
        buttonQuery.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvente) {
                try {
                    ClusterConnection clusterConnection = createClusterConfig();

                    if (Utilities.stringIsNullOrEmpty(textareaQuery.getText())) {
                        throw new RuntimeException("Query must be provided");
                    }

                    updateStatusTextProcessing();

                    String query = trimString(textareaQuery.getText());
                    QueryResult result = couchbase.getCluster(clusterConnection).query(query);
                    result.metaData();

                    // Get the results as pretty-printed content
                    List<JsonObject> resultContents = result.rowsAsObject();
                    String prettyJson = prettyPrintingWriter.writeValueAsString(resultContents);

                    textareaQueryResponse.setText(prettyJson);

                    config.upsertServer(clusterConnection, query);

                    updateStatusText("Query complete.");

                } catch (Exception e) {
                    updateStatusText(e);
                }
            }
        });

        comboClusterPicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateClusterUsers();
            }
        });

        comboboxUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePassword();
                updateBuckets();
            }
        });

        comboBucketName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateBucketCollections();
            }
        });
    }

    protected void updateClusterUsers() {
        comboboxUser.removeAllItems();

        String host = getSelectedText(comboClusterPicker);
        ClusterConfig clusterConfig = config.getClusterConfig(host);
        clusterConfig.getUsers().stream().forEach(user -> comboboxUser.addItem(user));
    }

    protected void updatePassword() {
        String host = getSelectedText(comboClusterPicker);
        ClusterConfig clusterConfig = config.getClusterConfig(host);

        UserConfig userConfig = clusterConfig.getUserConfig(getSelectedText(comboboxUser));

        if (userConfig != null) {
            password.setText(userConfig.getPassword());
        }
    }

    protected void updateBuckets() {
        comboBucketName.removeAllItems();

        String host = getSelectedText(comboClusterPicker);
        ClusterConfig clusterConfig = config.getClusterConfig(host);
        UserConfig userConfig = clusterConfig.getUserConfig(getSelectedText(comboboxUser));

        if (userConfig != null) {
            userConfig.getBuckets().stream().forEach(bucket -> comboBucketName.addItem(bucket));
        }
    }

    protected void updateBucketCollections() {
        comboboxCollection.removeAllItems();

        String host = getSelectedText(comboClusterPicker);
        ClusterConfig clusterConfig = config.getClusterConfig(host);
        UserConfig userConfig = clusterConfig.getUserConfig(getSelectedText(comboboxUser));

        if (userConfig != null) {
            List<String> collections = userConfig.getBucketCollections(getSelectedText(comboBucketName));
            collections.stream().forEach(collection -> comboboxCollection.addItem(collection));
        }
    }


    protected void loadConfig() {
        // Load the configuration
        try {
            config = ConfigFileManager.LoadConfig();

            // populate the combo box with cluster info (only)
            List<String> hostnames = config.getServerHostnames();
            hostnames.stream().forEach(hostname -> comboClusterPicker.addItem(hostname));
        } catch (Exception e) {
            updateStatusText(e);
            config = ConfigFileManager.CreateEmptyConfig();
        }

        // TODO: this causes problems
//        // Ensure nothing is selected by default

//        try {
//            comboClusterPicker.setSelectedItem("");
//        } catch (Exception e) {
//            // ignore
//        }
    }

    protected void saveConfig() {
        try {
            updateStatusText("Saving configuration...");
            ConfigFileManager.SaveConfig(config);
        } catch (Exception e) {
            updateStatusText(e);
        }
    }

    protected ClusterConnection createClusterConfig() {
        return new ClusterConnection(getSelectedText(comboClusterPicker),
                getSelectedText(comboboxUser), new String(password.getPassword()));
    }

    protected Collection getBucketCollection() throws IOException, JSONException {
        updateStatusText("Getting Couchbase...");

        ClusterConnection clusterConnection = createClusterConfig();
        BucketCollection bucketCollection = createBucketCollection();

        Collection collection = couchbase.getBucketCollection(clusterConnection, bucketCollection);

        // We did something successful, update the config
        config.upsertServer(clusterConnection, bucketCollection);

        // Ensure all the fields are populated in the UI dropdowns (inverse order is important)
        ensureComboboxContains(comboboxCollection, bucketCollection.getCollectionName());
        ensureComboboxContains(comboBucketName, bucketCollection.getBucketName());
        ensureComboboxContains(comboboxUser, clusterConnection.getUser());
        ensureComboboxContains(comboClusterPicker, clusterConnection.getHost());

        return collection;
    }

    private void ensureComboboxContains(JComboBox comboBox, String newItem) {
        // Don't add empty values to dropdowns
        if (newItem == null || newItem.isEmpty()) {
            return;
        }

        for (int i = 0; i < comboBox.getItemCount(); i++ ) {
            if (comboBox.getItemAt(i).equals(newItem)) {
                return;
            }
        }

        comboBox.addItem(newItem);
        comboBox.setSelectedItem(newItem);
    }

    protected BucketCollection createBucketCollection() {
        return new BucketCollection(getSelectedText(comboBucketName), getSelectedText(comboboxCollection));
    }

    protected String getSelectedText(JComboBox combobox) {
        return (combobox.getSelectedItem() != null) ? combobox.getSelectedItem().toString().trim() : "";
    }

    protected void updateStatusTextProcessing() {
        updateStatusText("Processing...");
    }

    protected void updateStatusText(String message) {
        updateStatusText(textStatusDisabledTextColor, textStatusBgColor, message);
    }

    protected void updateStatusText(Exception exception) {
        if (exception instanceof DocumentNotFoundException) {
            updateStatusText(Color.BLACK, Color.YELLOW, exception.getMessage());
        } else {
            updateStatusText(Color.WHITE, DARK_RED, exception.getMessage());
        }
    }

    private void updateStatusText(Color disabledTextColor, Color background, String message) {
        textStatus.setDisabledTextColor(disabledTextColor);
        textStatus.setBackground(background);
        textStatus.setText(message);
        textStatus.setToolTipText(message);
        textStatus.setCaretPosition(0);
        textStatus.update(textStatus.getGraphics());
    }

    public static void main(String[] args) {
        CouchbaseUI couchbaseUI = new CouchbaseUI();
        couchbaseUI.loadConfig();

        JFrame frame = new JFrame("CouchbaseUI");
        frame.setContentPane(couchbaseUI.panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Add window listener by implementing WindowAdapter class to
        // the frame instance. To handle the close event we just need
        // to implement the windowClosing() method.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                couchbaseUI.saveConfig();
            }
        });
    }
}