package com.refinepro.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;
import com.refinepro.app.ApplicationContext;
import com.refinepro.model.AuthenticationSettings;
import com.refinepro.model.Credentials;
import com.refinepro.utils.CredentialsEncryptor;
import com.refinepro.utils.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class GetAuthenticationTokenCommand extends Command {

    private final File settingsFile;
    private final File credentialsFile;

    public GetAuthenticationTokenCommand(ApplicationContext applicationContext) {
        this.settingsFile = applicationContext.getSettingsFile();
        this.credentialsFile = new File(settingsFile.getParentFile(), "credentials.json");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {

            ObjectMapper mapper = new ObjectMapper();
            Credentials credentials = mapper.readValue(credentialsFile, Credentials.class);
            credentials.auth_password = CredentialsEncryptor.decrypt(credentials.auth_password);
            AuthenticationSettings authenticationSettings = mapper.readValue(settingsFile, AuthenticationSettings.class);
            String message;
            String token = "";
            int code = 0;
            CloseableHttpClient httpclient = HttpClients.createDefault();
            try {
                if (authenticationSettings.auth_token_type != null && authenticationSettings.auth_token_type.equals("Basic")) {
                    HttpGet httpGet = new HttpGet(authenticationSettings.auth_endpoint);
                    CredentialsProvider provider = new BasicCredentialsProvider();
                    provider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(credentials.auth_username, credentials.auth_password));
                    httpclient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
                    CloseableHttpResponse httpResponse = httpclient.execute(httpGet);
                    code = httpResponse.getStatusLine().getStatusCode();
                    message = HttpStatus.getStatusText(httpResponse.getStatusLine().getStatusCode());
                    System.setProperty("OR_AUTH_USERNAME",credentials.auth_username);
                    System.setProperty("OR_AUTH_PASSWORD",credentials.auth_password);
                }else{
                    HttpPost httpPost = new HttpPost(authenticationSettings.auth_endpoint);
                    httpPost.setHeader("Content-Type", "application/json");

                    Map<String, String> credentialsMap = new HashMap<>();
                    credentialsMap.put(authenticationSettings.auth_username_property, credentials.auth_username);
                    credentialsMap.put(authenticationSettings.auth_password_property, credentials.auth_password);
                    StringEntity userEntity = new StringEntity(mapper.writeValueAsString(credentialsMap));
                    httpPost.setEntity(userEntity);

                    CloseableHttpResponse httpResponse = httpclient.execute(httpPost);
                    code = httpResponse.getStatusLine().getStatusCode();
                    message = HttpStatus.getStatusText(httpResponse.getStatusLine().getStatusCode());

                    JsonNode jsonResponse = mapper.readTree(EntityUtils.toString(httpResponse.getEntity()));
                    if (code == 200 && jsonResponse.has(authenticationSettings.auth_token_property)) {
                        token = authenticationSettings.auth_token_type + " " + jsonResponse.get(authenticationSettings.auth_token_property).asText("");
                    }
                }
            } catch (IOException e) {
                message = e.getLocalizedMessage();
            }


            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            Writer w = response.getWriter();
            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

            writer.writeStartObject();
            writer.writeNumberField("code", code);
            writer.writeStringField("message", message);
            writer.writeStringField("token", token);
            writer.writeStringField("header", authenticationSettings.auth_header);
            writer.writeEndObject();
            writer.flush();
            w.flush();
            w.close();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
