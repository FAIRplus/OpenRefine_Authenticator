package com.refinepro.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;
import com.refinepro.app.ApplicationContext;
import com.refinepro.model.AuthenticationSettings;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class SaveAuthenticationCommand extends Command {

    private File settingsFile;

    public SaveAuthenticationCommand(ApplicationContext applicationContext) {
        this.settingsFile = applicationContext.getSettingsFile();
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {

            ObjectMapper mapper = new ObjectMapper();
            AuthenticationSettings authenticationSettings = mapper.readValue(request.getReader(), AuthenticationSettings.class);
            mapper.writeValue(settingsFile, authenticationSettings);
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
