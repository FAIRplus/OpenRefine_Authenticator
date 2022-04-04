/*
    Custom code from original OpenRefine project main/src/com/google/refine/commands/recon/ExtendDataCommand.java
 */

package com.refinepro.commands.recon;

import javax.servlet.http.HttpServletRequest;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig;
import com.refinepro.operations.recon.ExtendDataOperation;

public class ExtendDataCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
                                                HttpServletRequest request, EngineConfig engineConfig) throws Exception {

        String baseColumnName = request.getParameter("baseColumnName");
        int columnInsertIndex = Integer.parseInt(request.getParameter("columnInsertIndex"));
        String endpoint = request.getParameter("endpoint");
        String identifierSpace = request.getParameter("identifierSpace");
        String schemaSpace = request.getParameter("schemaSpace");

        String jsonString = request.getParameter("extension");
//        job.authenticatorHeader = request.getParameter("authenticatorHeader");
//        job.authenticatorToken = request.getParameter("authenticatorToken");
        DataExtensionConfig extension = DataExtensionConfig.reconstruct(jsonString);
        ExtendDataOperation extendDataOperation = new ExtendDataOperation(
                engineConfig,
                baseColumnName,
                endpoint,
                identifierSpace,
                schemaSpace,
                extension,
                columnInsertIndex,
                request.getParameter("authenticatorHeader"),
                request.getParameter("authenticatorToken")
        );

        return extendDataOperation;
    }

}
