package com.refinepro.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refinepro.model.AuthenticationSettings;

import java.io.*;

public class ApplicationContext {

    private File settingsFile;

    protected void init(File workingDir) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(getClass().getResourceAsStream("DefaultConfiguration.json"));
        settingsFile = new File(workingDir, "configuration.json");
        if (settingsFile.exists() && settingsFile.isFile()){
            inputStreamReader = new InputStreamReader(new FileInputStream(settingsFile));
        }
        ObjectMapper mapper = new ObjectMapper();
        AuthenticationSettings authenticationSettings = mapper.readValue(inputStreamReader, AuthenticationSettings.class);
        mapper.writeValue(settingsFile, authenticationSettings);

    }


    public File getSettingsFile() {
        return settingsFile;
    }
}
