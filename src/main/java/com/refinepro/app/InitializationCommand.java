package com.refinepro.app;

import com.google.refine.RefineServlet;
import com.google.refine.commands.Command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class InitializationCommand extends Command {

    private File workingDir;

    @Override
    public void init(RefineServlet servlet) {
        workingDir = servlet.getCacheDir("authenticator/secrets");
        super.init(servlet);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request,response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        throw new UnsupportedOperationException("This command is not meant to be called. It is just necessary for initialization");
    }

    public void initApplicationContext(ApplicationContext applicationContext) throws IOException {
        applicationContext.init(workingDir);
    }


}
