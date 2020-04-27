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
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
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
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.desktop.*;
import java.awt.event.*;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

import static com.unhuman.couchbaseui.utils.Utilities.trimString;

public class CouchbaseUI implements AboutHandler, QuitHandler {
    private static final Color DARK_RED = new Color(128, 0, 0);
    private static final Color DARK_GREEN = new Color(0, 76, 0);
    private static final String NO_EXPIRY_STRING = "No Expiry";

    private final CouchbaseClientManager couchbase;
    private final ObjectWriter prettyPrintingWriter;
    private volatile CouchbaseUIConfig config;
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

    private AtomicBoolean isAboutDialogDisplayed;
    private boolean isConfigurationAlreadySaved;


    // TODO: Fix expiry tracking
    // private Duration currentExpiry;

    public CouchbaseUI() {
        // Create and deflect logs
        memoryHandler = new MemoryHandler2();
        isAboutDialogDisplayed = new AtomicBoolean(false);
        isConfigurationAlreadySaved = false;

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
                    JsonNode convertedObject = JacksonTransformers.MAPPER.readTree(textareaValue.getText());
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

                    JsonNode convertedObject = JacksonTransformers.MAPPER.readTree(textareaValue.getText());

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
                    for (currentQuery = queries.size(); !queries.get(currentQuery - 1).equals(query); currentQuery--) ;

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
                displayAboutDialog();
            }
        });
        buttonSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SettingsDialog.display(panel, config);
                adjustLogLevel("com.unhuman", config.getCouchbaseUILogLevel());
                adjustLogLevel("com.couchbase.client", config.getCouchbaseClientLogLevel());
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

                    if (forceShowIndex > 0) {
                        textAreaLogs.setCaretPosition(forceShowIndex);
                    }
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
            for (String user : clusterConfig.getUsers()) {
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
            for (String bucket : userConfig.getBucketNames()) {
                comboBucketName.addItem(bucket);
            }
        }
    }

    protected void updateBucketCollections() {
        comboboxCollection.removeAllItems();

        UserConfig userConfig = getUserConfig();
        if (userConfig != null) {
            List<String> collections = userConfig.getBucketCollections(getSelectedText(comboBucketName));
            for (String collection : collections) {
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
            for (String hostname : hostnames) {
                comboClusterPicker.addItem(hostname);
            }
            memoryHandler.setConfig(config);
            // Flush the logs - hibernate logs something we don't need to worry about
            memoryHandler.flush();

            adjustLogLevel("com.unhuman", config.getCouchbaseUILogLevel());
            adjustLogLevel("com.couchbase.client", config.getCouchbaseClientLogLevel());
        } catch (Exception e) {
            updateStatusText(e);
            config = ConfigFileManager.CreateEmptyConfig();
        } finally {
            frame.setEnabled(true);
        }

        updateStatusText("");
    }

    protected synchronized void saveConfig(JFrame frame) {
        if (!isConfigurationAlreadySaved) {
            isConfigurationAlreadySaved = true;

            if (config != null) {
                try {
                    frame.setEnabled(false);
                    updateStatusText("Saving configuration...");
                    ConfigFileManager.SaveConfig(config);
                    updateStatusText("Configuration saved.");
                } catch (Exception e) {
                    updateStatusText(e);
                } finally {
                    frame.setEnabled(true);
                }
            }
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
        for (insertItemLocation = 0; insertItemLocation < comboBox.getItemCount(); insertItemLocation++) {
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
        for (Handler h : logger.getParent().getHandlers()) {
            if (h instanceof ConsoleHandler) {
                logger.getParent().removeHandler(h);
                logger.getParent().addHandler(memoryHandler);
            }
        }
    }

    @Override
    public void handleAbout(AboutEvent e) {
        displayAboutDialog();
    }

    private void displayAboutDialog() {
        if (isAboutDialogDisplayed.compareAndSet(false, true)) {
            AboutDialog.display(panel);
            isAboutDialogDisplayed.set(false);
        }
    }

    @Override
    public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {
        saveConfig((JFrame) SwingUtilities.getWindowAncestor(panel));
        response.performQuit();
    }

    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "couchbaseUI");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ignore any problems here
        }

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

        Desktop.getDesktop().setAboutHandler(couchbaseUI);
        Desktop.getDesktop().setQuitHandler(couchbaseUI);

        frame.setVisible(true);

        couchbaseUI.loadConfig(frame);
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
        panel = new JPanel();
        panel.setLayout(new GridLayoutManager(1, 2, new Insets(5, 5, 5, 5), -1, -1));
        panel.setMinimumSize(new Dimension(740, 400));
        panel.setPreferredSize(new Dimension(740, 400));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(panel1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panelCluster = new JPanel();
        panelCluster.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panelCluster, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        final JLabel label1 = new JLabel();
        label1.setText("Cluster:");
        panelCluster.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setRequestFocusEnabled(false);
        label2.setText("User:");
        panelCluster.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), 0, 0));
        panelCluster.add(panel2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Password:");
        panel2.add(label3, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        password = new JPasswordField();
        panel2.add(password, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(175, -1), new Dimension(175, -1), new Dimension(175, -1), 0, false));
        comboboxUser = new JComboBox();
        comboboxUser.setEditable(true);
        panel2.add(comboboxUser, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(175, -1), new Dimension(175, -1), new Dimension(175, -1), 0, false));
        buttonDeleteUser = new JButton();
        buttonDeleteUser.setFocusable(false);
        buttonDeleteUser.setText("-");
        panel2.add(buttonDeleteUser, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(45, -1), new Dimension(45, -1), new Dimension(45, -1), 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonSettings = new JButton();
        buttonSettings.setLabel("Settings");
        buttonSettings.setText("Settings");
        panelCluster.add(buttonSettings, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), 0, 0));
        panelCluster.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, 1, null, null, null, 0, false));
        comboClusterPicker = new JComboBox();
        comboClusterPicker.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        comboClusterPicker.setModel(defaultComboBoxModel1);
        panel3.add(comboClusterPicker, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(420, -1), new Dimension(420, -1), new Dimension(420, -1), 0, false));
        buttonDeleteCluster = new JButton();
        buttonDeleteCluster.setFocusable(false);
        buttonDeleteCluster.setText("-");
        panel3.add(buttonDeleteCluster, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(45, -1), new Dimension(45, -1), new Dimension(45, -1), 0, false));
        final Spacer spacer2 = new Spacer();
        panelCluster.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        panelStatus = new JPanel();
        panelStatus.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panelStatus, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        final JLabel label4 = new JLabel();
        label4.setEnabled(false);
        label4.setText("Status:");
        panelStatus.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textStatus = new JTextField();
        textStatus.setColumns(2);
        textStatus.setEditable(false);
        textStatus.setEnabled(false);
        textStatus.setHorizontalAlignment(10);
        panelStatus.add(textStatus, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAbout = new JButton();
        buttonAbout.setText("About");
        panelStatus.add(buttonAbout, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabPanelOperations = new JTabbedPane();
        tabPanelOperations.setTabPlacement(1);
        panel1.add(tabPanelOperations, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tabKeyValue = new JPanel();
        tabKeyValue.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 5, 0), -1, -1));
        tabPanelOperations.addTab("Key/Value", tabKeyValue);
        panelKVActions = new JPanel();
        panelKVActions.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabKeyValue.add(panelKVActions, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        createButton = new JButton();
        createButton.setText("Create");
        panelKVActions.add(createButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fetchButton = new JButton();
        fetchButton.setText("Fetch");
        panelKVActions.add(fetchButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        updateButton = new JButton();
        updateButton.setText("Update");
        panelKVActions.add(updateButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setText("Delete");
        panelKVActions.add(deleteButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panelKVActions.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        panel4.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label5 = new JLabel();
        label5.setHorizontalAlignment(2);
        label5.setHorizontalTextPosition(2);
        label5.setText("TTL:");
        panel5.add(label5, BorderLayout.WEST);
        textFieldTTLAmount = new JTextField();
        textFieldTTLAmount.setMaximumSize(new Dimension(64, 30));
        panel5.add(textFieldTTLAmount, BorderLayout.CENTER);
        comboBoxTTLDurationType = new JComboBox();
        panel5.add(comboBoxTTLDurationType, BorderLayout.SOUTH);
        panelKVContent = new JPanel();
        panelKVContent.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabKeyValue.add(panelKVContent, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, true));
        panelMetadata = new JPanel();
        panelMetadata.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelKVContent.add(panelMetadata, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        textfieldMetadata = new JTextField();
        textfieldMetadata.setEditable(false);
        textfieldMetadata.setEnabled(false);
        panelMetadata.add(textfieldMetadata, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setEnabled(false);
        label6.setText("Metadata:");
        panelMetadata.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(30);
        panelKVContent.add(scrollPane1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textareaValue = new JTextArea();
        textareaValue.setLineWrap(false);
        scrollPane1.setViewportView(textareaValue);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelKVContent.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        textfieldDocumentKey = new JTextField();
        panel6.add(textfieldDocumentKey, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Key:");
        panel6.add(label7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Bucket:");
        panel6.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 7, new Insets(0, 0, 0, 0), 0, -1));
        panel6.add(panel7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        comboBucketName = new JComboBox();
        comboBucketName.setEditable(true);
        panel7.add(comboBucketName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Collection (optional):");
        panel7.add(label9, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboboxCollection = new JComboBox();
        comboboxCollection.setEditable(true);
        panel7.add(comboboxCollection, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        buttonDeleteBucket = new JButton();
        buttonDeleteBucket.setFocusable(false);
        buttonDeleteBucket.setText("-");
        panel7.add(buttonDeleteBucket, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(45, -1), new Dimension(45, -1), new Dimension(45, -1), 0, false));
        final Spacer spacer3 = new Spacer();
        panel7.add(spacer3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, 1, null, null, null, 0, false));
        buttonDeleteCollection = new JButton();
        buttonDeleteCollection.setFocusable(false);
        buttonDeleteCollection.setText("-");
        panel7.add(buttonDeleteCollection, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(45, -1), new Dimension(45, -1), new Dimension(45, -1), 0, false));
        final Spacer spacer4 = new Spacer();
        panel7.add(spacer4, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        tabN1QL = new JPanel();
        tabN1QL.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabPanelOperations.addTab("N1QL", tabN1QL);
        panelN1QL = new JPanel();
        panelN1QL.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabN1QL.add(panelN1QL, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setEnabled(false);
        label10.setText("Response:");
        panelN1QL.add(label10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new BorderLayout(0, 0));
        panelN1QL.add(panel8, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, BorderLayout.SOUTH);
        buttonQuery = new JButton();
        buttonQuery.setActionCommand("performN1qlQuery");
        buttonQuery.setLabel("Query");
        buttonQuery.setText("Query");
        panel9.add(buttonQuery, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel8.add(scrollPane2, BorderLayout.CENTER);
        textareaQuery = new JTextArea();
        scrollPane2.setViewportView(textareaQuery);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panelN1QL.add(panel10, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel10.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textareaQueryResponse = new JTextArea();
        textareaQueryResponse.setEditable(false);
        textareaQueryResponse.setEnabled(true);
        scrollPane3.setViewportView(textareaQueryResponse);
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panelN1QL.add(panel11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("Query:");
        panel11.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel11.add(spacer5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        labelQueryIndicator = new JLabel();
        labelQueryIndicator.setText("1/1");
        panel11.add(labelQueryIndicator, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel11.add(panel12, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonPrevQuery = new JButton();
        buttonPrevQuery.setHorizontalTextPosition(0);
        buttonPrevQuery.setMaximumSize(new Dimension(32, 30));
        buttonPrevQuery.setMinimumSize(new Dimension(32, 30));
        buttonPrevQuery.setPreferredSize(new Dimension(32, 30));
        buttonPrevQuery.setText("<");
        buttonPrevQuery.setVerticalAlignment(0);
        buttonPrevQuery.putClientProperty("hideActionText", Boolean.TRUE);
        panel12.add(buttonPrevQuery);
        buttonNextQuery = new JButton();
        buttonNextQuery.setFocusPainted(true);
        buttonNextQuery.setHorizontalTextPosition(0);
        buttonNextQuery.setMaximumSize(new Dimension(32, 30));
        buttonNextQuery.setMinimumSize(new Dimension(32, 30));
        buttonNextQuery.setOpaque(false);
        buttonNextQuery.setPreferredSize(new Dimension(32, 30));
        buttonNextQuery.setText(">");
        buttonNextQuery.setVerticalTextPosition(0);
        panel12.add(buttonNextQuery);
        buttonDeleteQuery = new JButton();
        buttonDeleteQuery.setText("Delete");
        panel11.add(buttonDeleteQuery, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel13.setName("tabPanelLogsTab");
        tabPanelOperations.addTab("Logs", panel13);
        final JScrollPane scrollPane4 = new JScrollPane();
        scrollPane4.setName("scrollpaneLogs");
        panel13.add(scrollPane4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textAreaLogs = new JTextArea();
        scrollPane4.setViewportView(textAreaLogs);
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel13.add(panel14, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonClearLogs = new JButton();
        buttonClearLogs.setText("Clear");
        panel14.add(buttonClearLogs, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel14.add(spacer6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        checkBoxLogsWordWrap = new JCheckBox();
        checkBoxLogsWordWrap.setLabel("Word Wrap");
        checkBoxLogsWordWrap.setText("Word Wrap");
        panel14.add(checkBoxLogsWordWrap, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }
}