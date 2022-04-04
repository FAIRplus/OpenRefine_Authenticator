package com.refinepro.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;
import com.refinepro.app.ApplicationContext;
import com.refinepro.model.Credentials;
import com.refinepro.utils.CredentialsEncryptor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class SaveCredentialsCommand extends Command {

    private File settingsFile;
    private File credentialsFile;

    public SaveCredentialsCommand(ApplicationContext applicationContext) {
        this.settingsFile = applicationContext.getSettingsFile();
        this.credentialsFile = new File(settingsFile.getParentFile(), "credentials.json");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {

            ObjectMapper mapper = new ObjectMapper();
            Credentials credentials = mapper.readValue(request.getReader(), Credentials.class);
            credentials.auth_password = CredentialsEncryptor.encrypt(credentials.auth_password);
            mapper.writeValue(credentialsFile, credentials);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            Writer w = response.getWriter();
            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

            writer.writeStartObject();
            writer.writeStringField("code", "ok");
            writer.writeEndObject();
            writer.flush();
            w.flush();
            w.close();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
