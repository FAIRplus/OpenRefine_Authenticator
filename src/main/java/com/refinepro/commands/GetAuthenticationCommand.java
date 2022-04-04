package com.refinepro.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.commands.Command;
import com.refinepro.app.ApplicationContext;
import com.refinepro.model.AuthenticationSettings;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class GetAuthenticationCommand extends Command {

    private File settingsFile;

    public GetAuthenticationCommand(ApplicationContext applicationContext) {
        this.settingsFile = applicationContext.getSettingsFile();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        try {
            getDefaultConfiguration(request, response);
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    private void getDefaultConfiguration(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        respondJSON(response, mapper.readValue(settingsFile, AuthenticationSettings.class));
    }
}
