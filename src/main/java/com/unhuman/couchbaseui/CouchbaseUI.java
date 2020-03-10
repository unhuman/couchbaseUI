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
import com.unhuman.couchbaseui.couchbase.CouchbaseClientManager;
import com.unhuman.couchbaseui.entities.BucketCollection;
import com.unhuman.couchbaseui.entities.ClusterConfig;
import com.unhuman.couchbaseui.utils.Utilities;
import us.monoid.json.JSONException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import static com.unhuman.couchbaseui.utils.Utilities.trimString;

public class CouchbaseUI {
    private static final Color DARK_RED = new Color(128, 0, 0);

    private final CouchbaseClientManager couchbase;
    private final ObjectWriter prettyPrintingWriter;

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
                    ClusterConfig clusterConfig = createClusterConfig();

                    if (Utilities.stringIsNullOrEmpty(textareaQuery.getText())) {
                        throw new RuntimeException("Query must be provided");
                    }

                    updateStatusTextProcessing();

                    String query = trimString(textareaQuery.getText());
                    QueryResult result = couchbase.getCluster(clusterConfig).query(query);
                    result.metaData();

                    // Get the results as pretty-printed content
                    List<JsonObject> resultContents = result.rowsAsObject();
                    String prettyJson = prettyPrintingWriter.writeValueAsString(resultContents);

                    textareaQueryResponse.setText(prettyJson);

                    updateStatusText("Query complete.");

                } catch (Exception e) {
                    updateStatusText(e);
                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("CouchbaseUI");
        frame.setContentPane(new CouchbaseUI().panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    protected ClusterConfig createClusterConfig() {
        return new ClusterConfig(getSelectedText(comboClusterPicker),
                getSelectedText(comboboxUser), new String(password.getPassword()));
    }

    protected Collection getBucketCollection() throws IOException, JSONException {
        updateStatusText("Getting Couchbase...");

        ClusterConfig clusterConfig = createClusterConfig();
        BucketCollection bucketCollection = createBucketCollection();

        return couchbase.getBucketCollection(clusterConfig, bucketCollection);
    }

    protected BucketCollection createBucketCollection() {
        return new BucketCollection(getSelectedText(comboBucketName), getSelectedText(comboboxCollection));
    }

    protected String getSelectedText(JComboBox combobox) {
        return  (combobox.getSelectedItem() != null) ? combobox.getSelectedItem().toString() : "";
    }

    protected void updateStatusTextProcessing() {
        updateStatusText("Processing...");
    }

    protected void updateStatusText(String message) {
        updateStatusText(textStatusDisabledTextColor, textStatusBgColor, message);
    }

    protected void updateStatusText(Exception exception) {
        updateStatusText(Color.WHITE, DARK_RED, exception.getMessage());
    }

    private void updateStatusText(Color disabledTextColor, Color background, String message) {
        textStatus.setDisabledTextColor(disabledTextColor);
        textStatus.setBackground(background);
        textStatus.setText(message);
        textStatus.setToolTipText(message);
        textStatus.setCaretPosition(0);
        textStatus.update(textStatus.getGraphics());
    }
}