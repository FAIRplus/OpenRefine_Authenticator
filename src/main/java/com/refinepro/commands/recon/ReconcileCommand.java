
package com.refinepro.commands.recon;

import javax.servlet.http.HttpServletRequest;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.operations.recon.ReconOperation;


public class ReconcileCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(Project project,
                                                HttpServletRequest request, EngineConfig engineConfig) throws Exception {
        String columnName = request.getParameter("columnName");
        String configString = request.getParameter("config");
        ReconConfig reconstruct = ReconConfig.reconstruct(configString);
        return new ReconOperation(engineConfig, columnName, reconstruct);
    }
}
