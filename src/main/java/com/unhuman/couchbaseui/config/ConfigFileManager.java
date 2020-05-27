package com.unhuman.couchbaseui.config;

import com.codingrodent.jackson.crypto.CryptoModule;
import com.codingrodent.jackson.crypto.EncryptionService;
import com.codingrodent.jackson.crypto.PasswordCryptoContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unhuman.couchbaseui.SecretDialog;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigFileManager {
    private static final File getConfigFile() {
        // TODO: Would be nice to use AppData on Windows
        return new File(System.getProperty("user.home") + "/.couchbaseUI.config");
    }

    public static CouchbaseUIConfig CreateEmptyConfig() {
        return CouchbaseUIConfig.CreateConfigFile(null);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static CouchbaseUIConfig LoadConfig(JPanel parentPanel) throws Exception {
        File file = getConfigFile();

        // We want to force failure (no loss) when trying initial load
        EncryptedConfigItem.SetPermitLossOfEncryptedValues(false);

        if (file.exists()) {
            // read in file contents (so we don't have to reload same file)
            String fileContents = String.join("", Files.readAllLines(file.toPath()));

            try {
                return GetObjectMapper(null).readValue(fileContents, CouchbaseUIConfig.class);
            } catch (JsonMappingException jme) {
                // This is an encrypted file, so prompt for password and read it in
                do {
                    try {
                        AtomicReference<String> secret = new AtomicReference<>();

                        SecretDialog.display(parentPanel, secret);

                        EncryptedConfigItem.SetPermitLossOfEncryptedValues(secret.get() == null);
                        CouchbaseUIConfig config =
                                GetObjectMapper(secret.get()).readValue(fileContents, CouchbaseUIConfig.class);
                        config.setSecret(secret.get());

                        return config;
                    } catch (JsonMappingException jme2) {
                        // Do nothing - loop will continue
                    }
                } while (true);
            } finally {
                // Allow passwords to be managed during processing
                EncryptedConfigItem.SetPermitLossOfEncryptedValues(false);
            }
        } else {
            // file didn't exist, just return nothing
            return CreateEmptyConfig();
        }
    }

    public static void SaveConfig(CouchbaseUIConfig config) throws Exception {
        // Remove users that have not been used successfully
        config.getServerHostnames().stream().forEach(serverHostname -> {
            ClusterConfig clusterConfig = config.getClusterConfig(serverHostname);
            clusterConfig.getUsers().stream().forEach(user -> {
                if (!clusterConfig.getUserConfig(user).isValidated()) {
                    clusterConfig.removeUser(user);
                }
            });
        });

        File file = getConfigFile();
        EncryptedConfigItem.SetPermitLossOfEncryptedValues(config.getSecret() == null);
        GetObjectMapper(config.getSecret()).writerWithDefaultPrettyPrinter().writeValue(file, config);
    }

    private static ObjectMapper GetObjectMapper(String secret) {
        // get an object mapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (secret != null) {
            // set up a custom crypto context - Defines the interface to the crypto algorithms used
            PasswordCryptoContext cryptoContext = new PasswordCryptoContext(secret);
            // The encryption service holds functionality to map clear to / from encrypted JSON
            EncryptionService encryptionService = new EncryptionService(objectMapper, cryptoContext);
            // Create a Jackson module and tell it about the encryption service
            CryptoModule cryptoModule = new CryptoModule().addEncryptionService(encryptionService);
            // Tell Jackson about the new module
            objectMapper.registerModule(cryptoModule);
        }
        return objectMapper;
    }
}
