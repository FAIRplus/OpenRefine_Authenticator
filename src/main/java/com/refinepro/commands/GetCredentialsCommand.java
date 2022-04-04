package com.refinepro.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.commands.Command;
import com.refinepro.app.ApplicationContext;
import com.refinepro.model.Credentials;
import com.refinepro.utils.CredentialsEncryptor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class GetCredentialsCommand extends Command {

    private File settingsFile;
    private File credentialsFile;

    public GetCredentialsCommand(ApplicationContext applicationContext) {
        this.settingsFile = applicationContext.getSettingsFile();

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        ObjectMapper mapper = new ObjectMapper();
        try {

            this.credentialsFile = new File(settingsFile.getParentFile(), "credentials.json");
            if (credentialsFile.exists()) {
                Credentials credentials = mapper.readValue(credentialsFile, Credentials.class);
                credentials.auth_password = CredentialsEncryptor.decrypt(credentials.auth_password);
                respondJSON(response, credentials);
            }else{
                respondJSON(response, mapper.readValue("{}", Credentials.class));
            }

        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
