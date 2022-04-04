/*
    Custom code from original OpenRefine project main/src/com/google/refine/commands/recon/PreviewExtendDataCommand.java
*/

package com.refinepro.commands.recon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.refine.commands.Command;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Row;
import com.google.refine.model.recon.ReconConfig;
import com.refinepro.model.recon.ReconciledDataExtensionJob;
import com.refinepro.model.recon.ReconciledDataExtensionJob.ColumnInfo;
import com.google.refine.util.ParsingUtilities;
import com.refinepro.model.recon.StandardReconConfig;

public class PreviewExtendDataCommand extends Command {

    protected static class PreviewResponse {
        public PreviewResponse(List<ColumnInfo> columns2, List<List<Object>> rows2) {
            columns = columns2;
            rows = rows2;
        }

        @JsonProperty("code")
        protected String code = "ok";
        @JsonProperty("columns")
        protected List<ColumnInfo> columns;
        @JsonProperty("rows")
        protected List<List<Object>> rows;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);
            String columnName = request.getParameter("columnName");

            String rowIndicesString = request.getParameter("rowIndices");
            if (rowIndicesString == null) {
                respond(response, "{ \"code\" : \"error\", \"message\" : \"No row indices specified\" }");
                return;
            }

            String jsonString = request.getParameter("extension");
            com.google.refine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig config = com.google.refine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig.reconstruct(jsonString);

            List<Integer> rowIndices = ParsingUtilities.mapper.readValue(rowIndicesString, new TypeReference<List<Integer>>() {
            });
            int length = rowIndices.size();
            Column column = project.columnModel.getColumnByName(columnName);
            int cellIndex = column.getCellIndex();

            // get the endpoint to extract data from
            String endpoint = null;
            ReconConfig cfg = column.getReconConfig();
            if (cfg != null &&
                    cfg instanceof StandardReconConfig) {
                StandardReconConfig scfg = (StandardReconConfig) cfg;
                endpoint = scfg.service;
            } else {
                respond(response, "{ \"code\" : \"error\", \"message\" : \"This column has not been reconciled with a standard service.\" }");
                return;
            }


            List<String> topicNames = new ArrayList<String>();
            List<String> topicIds = new ArrayList<String>();
            Set<String> ids = new HashSet<String>();
            for (int i = 0; i < length; i++) {
                int rowIndex = rowIndices.get(i);
                if (rowIndex >= 0 && rowIndex < project.rows.size()) {
                    Row row = project.rows.get(rowIndex);
                    Cell cell = row.getCell(cellIndex);
                    if (cell != null && cell.recon != null && cell.recon.match != null) {
                        topicNames.add(cell.recon.match.name);
                        topicIds.add(cell.recon.match.id);
                        ids.add(cell.recon.match.id);
                    } else {
                        topicNames.add(null);
                        topicIds.add(null);
                        ids.add(null);
                    }
                }
            }

            Map<String, ReconCandidate> reconCandidateMap = new HashMap<String, ReconCandidate>();
            ReconciledDataExtensionJob job = new ReconciledDataExtensionJob(config, endpoint);
            job.authenticatorHeader = request.getParameter("authenticatorHeader");
            job.authenticatorToken = request.getParameter("authenticatorToken");
            Map<String, com.google.refine.model.recon.ReconciledDataExtensionJob.DataExtension> map = job.extend(ids, reconCandidateMap);
            List<List<Object>> rows = new ArrayList<>();

            for (int r = 0; r < topicNames.size(); r++) {
                String id = topicIds.get(r);
                String topicName = topicNames.get(r);

                if (id != null && map.containsKey(id)) {
                    com.google.refine.model.recon.ReconciledDataExtensionJob.DataExtension ext = map.get(id);
                    boolean first = true;

                    if (ext.data.length > 0) {
                        for (Object[] row : ext.data) {
                            List<Object> jsonRow = new ArrayList<>();
                            if (first) {
                                jsonRow.add(topicName);
                                first = false;
                            } else {
                                jsonRow.add(null);
                            }

                            for (Object cell : row) {
                                jsonRow.add(cell);
                            }
                            rows.add(jsonRow);
                        }
                        continue;
                    }
                }

                List<Object> supplement = new ArrayList<>();
                if (id != null) {
                    supplement.add(new ReconCandidate(id, topicName, new String[0], 100));
                } else {
                    supplement.add("<not reconciled>");
                }
                rows.add(supplement);
            }

            respondJSON(response, new PreviewResponse(job.columns, rows));
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
