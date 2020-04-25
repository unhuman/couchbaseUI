package com.unhuman.couchbaseui;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.util.DefaultIndenter;
import com.couchbase.client.core.deps.com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.JsonNode;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectWriter;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.SerializationFeature;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.core.error.FeatureNotAvailableException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JacksonTransformers;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.*;
import com.couchbase.client.java.query.QueryResult;
import com.unhuman.couchbaseui.config.ClusterConfig;
import com.unhuman.couchbaseui.config.ConfigFileManager;
import com.unhuman.couchbaseui.config.CouchbaseUIConfig;
import com.unhuman.couchbaseui.config.UserConfig;
import com.unhuman.couchbaseui.couchbase.CouchbaseClientManager;
import com.unhuman.couchbaseui.entities.BucketCollection;
import com.unhuman.couchbaseui.entities.ClusterConnection;
import com.unhuman.couchbaseui.exceptions.BadInputException;
import com.unhuman.couchbaseui.logging.MemoryHandler2;
import com.unhuman.couchbaseui.utils.Utilities;
import us.monoid.json.JSONException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.*;

import static com.unhuman.couchbaseui.utils.Utilities.trimString;
import static java.awt.SystemColor.menu;

public class CouchbaseUI {
    private static final Color DARK_RED = new Color(128, 0, 0);
    private static final Color DARK_GREEN = new Color(0, 76, 0);
    private static final String NO_EXPIRY_STRING = "No Expiry";

    private final CouchbaseClientManager couchbase;
    private final ObjectWriter prettyPrintingWriter;
    private CouchbaseUIConfig config;
    private int currentQuery;

    // UI created by IntelliJ
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
    private JButton buttonPrevQuery;
    private JButton buttonNextQuery;
    private JLabel labelQueryIndicator;
    private JButton buttonAbout;
    private JButton buttonDeleteQuery;
    private JComboBox comboBoxTTLDurationType;
    private JTextField textFieldTTLAmount;
    private JButton buttonSettings;
    private JButton buttonDeleteCluster;
    private JButton buttonDeleteUser;
    private JButton buttonDeleteBucket;
    private JButton buttonDeleteCollection;
    private JTextArea textAreaLogs;
    private JButton buttonClearLogs;
    private JCheckBox checkBoxLogsWordWrap;

    private final Color textStatusDisabledTextColor;
    private final Color textStatusBgColor;

    private long currentCAS = -1L;

    // Flag to indicate the UI is updating itself, so don't process events
    private boolean isUISelfUpdating = false;

    private MemoryHandler2 memoryHandler;

    // TODO: Fix expiry tracking
    // private Duration currentExpiry;

    public CouchbaseUI() {
        // Create and deflect logs
        memoryHandler = new MemoryHandler2();

        // https://stackoverflow.com/questions/311408/turning-off-hibernate-logging-console-output/25768383#25768383
        //magical - do not touch
        @SuppressWarnings("unused")
        org.jboss.logging.Logger logger = org.jboss.logging.Logger.getLogger("org.hibernate");

        // set simple default log levels
        adjustLogLevel("com.unhuman", Level.INFO);
        adjustLogLevel("com.couchbase.client", Level.OFF);
        adjustLogLevel("org.hibernate", Level.OFF);

        // Set the global mapper to pretty print
        JacksonTransformers.MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        prettyPrintingWriter = JacksonTransformers.MAPPER.writer(prettyPrinter);

        // Backup current colors for status so it can be restored.
        textStatusDisabledTextColor = textStatus.getDisabledTextColor();
        textStatusBgColor = textStatus.getBackground();

        couchbase = new CouchbaseClientManager();

        currentQuery = 1;

        comboBoxTTLDurationType.addItem(ChronoUnit.SECONDS);
        comboBoxTTLDurationType.addItem(ChronoUnit.MINUTES);
        comboBoxTTLDurationType.addItem(ChronoUnit.HOURS);
        comboBoxTTLDurationType.addItem(ChronoUnit.DAYS);
        comboBoxTTLDurationType.addItem(ChronoUnit.YEARS);
        comboBoxTTLDurationType.addItem(NO_EXPIRY_STRING);

        /** Set up all the listeners */

        // Setup KV Create Document button
        createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Collection collection = getBucketCollection();

                    if (Utilities.stringIsNullOrEmpty(textfieldDocumentKey.getText())) {
                        throw new BadInputException("Key must be provided");
                    }

                    if (Utilities.stringIsNullOrEmpty(textareaValue.getText())) {
                        throw new BadInputException("Content must be provided");
                    }

                    updateStatusTextProcessing();

                    String documentKey = trimString(textfieldDocumentKey.getText());
                    JsonNode convertedObject = JacksonTransformers.MAPPER. readTree(textareaValue.getText());
                    Duration expiryDuration = calculateExpiry();
                    InsertOptions insertOptions = InsertOptions.insertOptions().expiry(expiryDuration);
                    MutationResult result = collection.insert(documentKey, convertedObject, insertOptions);
                    currentCAS = result.cas();

                    // TODO: set currentExpiry
                    // currentExpiry = ttlDuration;

                    updateStatusSuccess("Create complete.");
                } catch (Exception e) {
                    handleBucketCRUDException(e);
                }
            }
        });
        // Setup KV Fetch Document button
        fetchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Collection collection = getBucketCollection();

                    if (Utilities.stringIsNullOrEmpty(textfieldDocumentKey.getText())) {
                        throw new BadInputException("Key must be provided");
                    }

                    updateStatusTextProcessing();

                    String documentKey = trimString(textfieldDocumentKey.getText());
                    GetResult result = collection.get(documentKey);
                    textfieldMetadata.setText("CAS: " + result.cas() + " Expiry: " + result.expiry());

                    JsonObject resultContent = result.contentAsObject();
                    String prettyJson = prettyPrintingWriter.writeValueAsString(resultContent);

                    textareaValue.setText(prettyJson);
                    currentCAS = result.cas();

                    // TODO: set currentExpiry (this fails for now)
                    // currentExpiry = result.expiry().get();

                    updateStatusSuccess("Fetch complete.");
                } catch (Exception e) {
                    handleBucketCRUDException(e);
                }
            }
        });

        // Setup KV Update Document button
        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Collection collection = getBucketCollection();

                    if (Utilities.stringIsNullOrEmpty(textfieldDocumentKey.getText())) {
                        throw new BadInputException("Key must be provided");
                    }

                    if (Utilities.stringIsNullOrEmpty(textareaValue.getText())) {
                        throw new BadInputException("Content must be provided");
                    }

                    updateStatusTextProcessing();

                    String documentKey = trimString(textfieldDocumentKey.getText());

                    // TODO: Right now, we always use the TTL provided
                    Duration expiry = calculateExpiry();
                    ReplaceOptions replaceOptions = ReplaceOptions.replaceOptions()
                            .cas(currentCAS)
                            .expiry(expiry);

                    JsonNode convertedObject = JacksonTransformers.MAPPER. readTree(textareaValue.getText());

                    MutationResult result = collection.replace(documentKey, convertedObject, replaceOptions);
                    currentCAS = result.cas();

                    updateStatusSuccess("Update complete.");
                } catch (Exception e) {
                    handleBucketCRUDException(e);
                }
            }
        });

        // Setup KV Delete Document button
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    Collection collection = getBucketCollection();

                    if (Utilities.stringIsNullOrEmpty(textfieldDocumentKey.getText())) {
                        throw new BadInputException("Key must be provided");
                    }

                    updateStatusTextProcessing();

                    String documentKey = trimString(textfieldDocumentKey.getText());
                    RemoveOptions removeOptions = RemoveOptions.removeOptions()
                            .cas(currentCAS);
                    collection.remove(documentKey, removeOptions);

                    textareaValue.setText("");

                    updateStatusSuccess("Delete complete.");
                } catch (DocumentNotFoundException dnfe) {
                    textareaValue.setText("");

                    updateStatusText(dnfe);
                } catch (Exception e) {
                    handleBucketCRUDException(e);
                }
            }
        });

        // Setup N1QL Query button
        buttonQuery.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvente) {
                try {
                    ClusterConnection clusterConnection = createClusterConfig();

                    if (Utilities.stringIsNullOrEmpty(textareaQuery.getText())) {
                        throw new BadInputException("Query must be provided");
                    }

                    updateStatusTextProcessing();

                    String query = trimString(textareaQuery.getText());
                    QueryResult result = couchbase.getCluster(panel, clusterConnection).query(query);
                    result.metaData();

                    // Get the results as pretty-printed content
                    List<JsonObject> resultContents = result.rowsAsObject();
                    String prettyJson = prettyPrintingWriter.writeValueAsString(resultContents);

                    textareaQueryResponse.setText(prettyJson);

                    List<String> queries = getUserConfig().getQueries();
                    config.upsertServer(clusterConnection, query);

                    // Find the query we just ran and indicate it to be the current one
                    for (currentQuery = queries.size(); !queries.get(currentQuery - 1).equals(query); currentQuery--);

                    updateQueryStatus();

                    updateStatusSuccess("Query complete.");

                } catch (Exception e) {
                    updateStatusText(e);
                }
            }
        });

        comboClusterPicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Don't process when UI is self updating
                if (isUISelfUpdating) {
                    return;
                }
                updateClusterUsers();
            }
        });

        comboboxUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Don't process when UI is self updating
                if (isUISelfUpdating) {
                    return;
                }
                updatePassword();
                updateBuckets();
                updateQueries();
            }
        });

        comboBucketName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Don't process when UI is self updating
                if (isUISelfUpdating) {
                    return;
                }
                updateBucketCollections();
            }
        });
        buttonPrevQuery.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentQuery--;
                displayCurrentQuery();
            }
        });
        buttonNextQuery.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentQuery++;
                displayCurrentQuery();
            }
        });
        buttonAbout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutDialog.display(panel);
            }
        });
        buttonSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SettingsDialog.display(panel, config);
            }
        });
        buttonDeleteCluster.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCurrentCluster();
            }
        });
        buttonDeleteUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCurrentUser();
            }
        });
        buttonDeleteBucket.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCurrentBucket();
            }
        });
        buttonDeleteQuery.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCurrentQuery();
            }
        });
        buttonDeleteCollection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCurrentCollection();
            }
        });
        tabPanelOperations.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if ("Logs".equals(tabPanelOperations.getTitleAt(tabPanelOperations.getSelectedIndex()))) {
                    textAreaLogs.removeAll();
                    textAreaLogs.setText(String.join("\n", memoryHandler.getMessages()));
                    checkBoxLogsWordWrap.setSelected(config.isLogsWordWrap());
                    textAreaLogs.setLineWrap(config.isLogsWordWrap());

                    // Display the bottom left of the logs (varies if low wrap or not)
                    int forceShowIndex;
                    if (config.isLogsWordWrap()) {
                        forceShowIndex = textAreaLogs.getText().length();
                    } else {
                        forceShowIndex = textAreaLogs.getText().lastIndexOf('\n');
                        if (forceShowIndex > 0) {
                            forceShowIndex = forceShowIndex + 1;
                        }
                    }
                    textAreaLogs.setCaretPosition(forceShowIndex);
                }
            }
        });
        buttonClearLogs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                memoryHandler.flush();
                textAreaLogs.setText("");
            }
        });
        checkBoxLogsWordWrap.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setLogsWordWrap(checkBoxLogsWordWrap.isSelected());
                textAreaLogs.setLineWrap(checkBoxLogsWordWrap.isSelected());
            }
        });
    }

    protected void handleBucketCRUDException(Exception e) {
        String exceptionText = e.toString();

        updateStatusText(e);

        // When there's bad input, don't clear out the UI
        if (e instanceof BadInputException) {
            return;
        }

        if (e instanceof FeatureNotAvailableException) {
            deleteCurrentCollection();
        }

        if (exceptionText.contains("BUCKET_NOT_AVAILABLE")) {
            // this is when the bucket doesn't exist
            deleteCurrentBucket();
        }

        textareaValue.setText("");
    }

    protected void updateClusterUsers() {
        comboboxUser.removeAllItems();
        password.setText("");

        ClusterConfig clusterConfig = getClusterConfig();
        if (clusterConfig != null) {
            for (String user: clusterConfig.getUsers()) {
                comboboxUser.addItem(user);
            }
        }
    }

    protected void updatePassword() {
        UserConfig userConfig = getUserConfig();
        if (userConfig != null) {
            password.setText(userConfig.getPassword());
        }
    }

    protected void updateBuckets() {
        comboBucketName.removeAllItems();

        UserConfig userConfig = getUserConfig();

        if (userConfig != null) {
            for (String bucket: userConfig.getBucketNames()) {
                comboBucketName.addItem(bucket);
            }
        }
    }

    protected void updateBucketCollections() {
        comboboxCollection.removeAllItems();

        UserConfig userConfig = getUserConfig();
        if (userConfig != null) {
            List<String> collections = userConfig.getBucketCollections(getSelectedText(comboBucketName));
            for (String collection: collections) {
                comboboxCollection.addItem(collection);
            }
        }

        // wipe out any data in the k/v tab
        textfieldDocumentKey.setText("");
        textfieldMetadata.setText("");
        textareaValue.setText("");
    }

    protected void updateQueries() {
        UserConfig userConfig = getUserConfig();
        if (userConfig != null) {
            List<String> queries = userConfig.getQueries();
            currentQuery = queries.size();
        }
        displayCurrentQuery();
    }

    protected void deleteCurrentQuery() {
        UserConfig userConfig = getUserConfig();
        if (userConfig != null) {
            List<String> queries = userConfig.getQueries();
            queries.remove(currentQuery - 1);
            if (currentQuery > queries.size()) {
                currentQuery = queries.size();
                if (currentQuery == 0) {
                    currentQuery = 1;
                }
            }
            displayCurrentQuery();
        }
    }


    protected void displayCurrentQuery() {
        UserConfig userConfig = getUserConfig();
        String useQuery = "";
        if (userConfig != null) {
            List<String> queries = userConfig.getQueries();
            useQuery = (queries.size() > 0) ? queries.get(currentQuery - 1) : "";
        }

        textareaQuery.setText(useQuery);
        currentQuery = useQuery.isEmpty() ? 1 : currentQuery;

        textareaQueryResponse.setText("");

        updateQueryStatus();
    }

    protected void updateQueryStatus() {
        UserConfig userConfig = getUserConfig();
        int maxQuery = 1;
        if (userConfig != null) {
            List<String> queries = userConfig.getQueries();
            maxQuery = Math.max(1, queries.size());
        } else {
            textareaQuery.setText("");
        }

        labelQueryIndicator.setText(String.format("%d/%d", currentQuery, maxQuery));
        buttonPrevQuery.setEnabled(currentQuery > 1);
        buttonNextQuery.setEnabled(currentQuery < maxQuery);
        buttonDeleteQuery.setEnabled(buttonPrevQuery.isEnabled()
                || buttonNextQuery.isEnabled()
                || !textareaQuery.getText().trim().isEmpty());
    }

    protected void loadConfig(JFrame frame) {
        updateStatusText("Loading configuration...");

        // Load the configuration
        try {
            frame.setEnabled(false);
            config = ConfigFileManager.LoadConfig(panel);

            // populate the combo box with cluster info (only)
            List<String> hostnames = config.getServerHostnames();
            for (String hostname: hostnames) {
                comboClusterPicker.addItem(hostname);
            }
        } catch (Exception e) {
            updateStatusText(e);
            config = ConfigFileManager.CreateEmptyConfig();
        } finally {
            frame.setEnabled(true);
        }

        updateStatusText("");
    }

    protected void saveConfig(JFrame frame) {
        try {
            frame.setEnabled(false);
            updateStatusText("Saving configuration...");
            ConfigFileManager.SaveConfig(config);
        } catch (Exception e) {
            updateStatusText(e);
        } finally {
            frame.setEnabled(true);
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

        Collection collection = couchbase.getBucketCollection(panel, clusterConnection, bucketCollection);

        // We did something successful, update the config
        config.upsertServer(clusterConnection, bucketCollection);

        try {
            isUISelfUpdating = true;

            // Ensure all the fields are populated in the UI dropdowns (inverse order is important)
            ensureComboboxContains(comboboxCollection, bucketCollection.getCollectionName());
            ensureComboboxContains(comboBucketName, bucketCollection.getBucketName());
            ensureComboboxContains(comboboxUser, clusterConnection.getUser());
            ensureComboboxContains(comboClusterPicker, clusterConnection.getHost());
        } finally {
            isUISelfUpdating = false;
        }

        return collection;
    }

    private void ensureComboboxContains(JComboBox comboBox, String newItem) {
        // Don't add empty values to dropdowns
        if (newItem == null || newItem.isEmpty()) {
            return;
        }

        int insertItemLocation;
        for (insertItemLocation = 0; insertItemLocation < comboBox.getItemCount(); insertItemLocation++ ) {
            String item = comboBox.getItemAt(insertItemLocation).toString();
            int comparison = item.compareTo(newItem);
            if (comparison == 0) {
                return;
            }

            if (comparison > 0) {
                break;
            }
        }

        comboBox.insertItemAt(newItem, insertItemLocation);
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

    protected void updateStatusSuccess(String message) {
        updateStatusText(DARK_GREEN, textStatusBgColor, message);
    }

    protected void updateStatusText(Exception exception) {
        String exceptionText = exception.getMessage();
        if ((exception instanceof IOException) && exceptionText.contains("closed")) {
            exceptionText += ": Likely invalid password";
        }

        if (exception instanceof DocumentNotFoundException) {
            updateStatusText(Color.BLACK, Color.YELLOW, exceptionText);
        } else {
            // Clean off the package from the exception text
            exceptionText = exceptionText.replaceFirst("([^:])*\\.", "");
            updateStatusText(Color.WHITE, DARK_RED, exceptionText);
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

    protected ClusterConfig getClusterConfig() {
        String host = getSelectedText(comboClusterPicker);
        ClusterConfig clusterConfig = config.getClusterConfig(host);
        return clusterConfig;
    }


    protected UserConfig getUserConfig() {
        ClusterConfig clusterConfig = getClusterConfig();
        UserConfig userConfig = (clusterConfig != null)
                ? clusterConfig.getUserConfig(getSelectedText(comboboxUser))
                : null;
        return userConfig;
    }

    private Duration calculateExpiry() {
        long expiryNumeric;
        try {
            expiryNumeric = Long.parseLong(textFieldTTLAmount.getText().trim());
        } catch (Exception e) {
            throw new BadInputException("Problem parsing TTL.");
        }
        if (comboBoxTTLDurationType.getSelectedItem() instanceof ChronoUnit) {
            if (expiryNumeric <= 0) {
                throw new BadInputException("Provided TTL must be positive value with time period.");
            }
            return Duration.of(expiryNumeric, (ChronoUnit) comboBoxTTLDurationType.getSelectedItem());
        } else if (comboBoxTTLDurationType.getSelectedItem().equals(NO_EXPIRY_STRING)) {
            if (expiryNumeric != 0) {
                throw new BadInputException("Provided TTL must be 0 when provided with " + NO_EXPIRY_STRING + ".");
            }
            return Duration.ZERO;
        } else {
            throw new BadInputException("Unexpected TTL Type: " + comboBoxTTLDurationType.getSelectedItem());
        }
    }

    private ClusterConfig getCurrentClusterConfig() {
        Object selectedItem = comboClusterPicker.getSelectedItem();
        return (selectedItem != null) ? config.getClusterConfig(selectedItem.toString()) : null;
    }

    private UserConfig getCurrentUserConfig() {
        Object selectedItem = comboboxUser.getSelectedItem();
        return (getCurrentClusterConfig() != null && selectedItem != null)
                ? getCurrentClusterConfig().getUserConfig(selectedItem.toString()) : null;
    }

    protected void deleteCurrentCluster() {
        Object selectedItem = comboClusterPicker.getSelectedItem();
        if (selectedItem != null) {
            String clusterRemove = selectedItem.toString();
            config.removeCluster(clusterRemove);
            comboClusterPicker.removeItem(selectedItem);
        }
        comboClusterPicker.setSelectedIndex(-1);
        deleteCurrentUser();
    }

    protected void deleteCurrentUser() {
        Object selectedItem = comboboxUser.getSelectedItem();
        if (selectedItem != null) {
            String userRemove = selectedItem.toString();
            if (getCurrentClusterConfig() != null) {
                getCurrentClusterConfig().removeUser(userRemove);
            }
            comboboxUser.removeItem(selectedItem);
        }
        comboboxUser.setSelectedIndex(-1);
        password.setText("");
        deleteCurrentBucket();
        deleteCurrentQuery();
    }

    protected void deleteCurrentBucket() {
        Object selectedItem = comboBucketName.getSelectedItem();
        if (selectedItem != null) {
            String bucketRemove = selectedItem.toString();
            if (getCurrentUserConfig() != null) {
                getCurrentUserConfig().removeBucket(bucketRemove);
            }
            comboBucketName.removeItem(selectedItem);
        }
        comboBucketName.setSelectedIndex(-1);
        deleteCurrentCollection();
    }

    protected void deleteCurrentCollection() {
        Object selectedBucket = comboBucketName.getSelectedItem();
        Object selectedCollection = comboboxCollection.getSelectedItem();
        if (selectedCollection != null) {
            String collectionRemove = selectedCollection.toString();
            if (selectedBucket != null && getUserConfig() != null) {
                getCurrentUserConfig().removeBucketCollection(selectedBucket.toString(), collectionRemove);
            }
            comboboxCollection.removeItem(selectedCollection);
        }
        comboboxCollection.setSelectedIndex(-1);
    }

    private void adjustLogLevel(String loggerFilter, Level level) {
        Logger logger = Logger.getLogger(loggerFilter);
        logger.setLevel(level);
        for(Handler h : logger.getParent().getHandlers()) {
            if(h instanceof ConsoleHandler) {
                logger.getParent().removeHandler(h);
                logger.getParent().addHandler(memoryHandler);
            }
        }
    }

    public static void main(String[] args) {
        CouchbaseUI couchbaseUI = new CouchbaseUI();

        JFrame frame = new JFrame("CouchbaseUI");
        frame.setContentPane(couchbaseUI.panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationByPlatform(true);
        frame.pack();
        frame.setPreferredSize(frame.getSize());
        frame.setMinimumSize(frame.getSize());

        // Add window listener by implementing WindowAdapter class to
        // the frame instance. To handle the close event we just need
        // to implement the windowClosing() method.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                couchbaseUI.saveConfig(frame);
            }
        });

        frame.setVisible(true);

        couchbaseUI.loadConfig(frame);
    }
}