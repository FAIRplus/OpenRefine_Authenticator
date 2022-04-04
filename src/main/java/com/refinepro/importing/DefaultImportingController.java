package com.refinepro.importing;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.importing.ImportingManager.Format;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

/*
    Custom code from original OpenRefine project main/src/com/google/refine/importing/DefaultImportingController.java
*/
public class DefaultImportingController extends com.google.refine.importing.DefaultImportingController {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        /*
         * The uploaded file is in the POST body as a "file part". If
         * we call request.getParameter() then the POST body will get
         * read and we won't have a chance to parse the body ourselves.
         * This is why we have to parse the URL for parameters ourselves.
         */
        Properties parameters = ParsingUtilities.parseUrlParameters(request);
        String subCommand = parameters.getProperty("subCommand");
        if ("load-raw-data".equals(subCommand)) {
            doLoadRawData(request, response, parameters);
        } else if ("update-file-selection".equals(subCommand)) {
            doUpdateFileSelection(request, response, parameters);
        } else if ("initialize-parser-ui".equals(subCommand)) {
            doInitializeParserUI(request, response, parameters);
        } else if ("update-format-and-options".equals(subCommand)) {
            doUpdateFormatAndOptions(request, response, parameters);
        } else if ("create-project".equals(subCommand)) {
            doCreateProject(request, response, parameters);
        } else {
            HttpUtilities.respond(response, "error", "No such sub command");
        }
    }

    private void doLoadRawData(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        ObjectNode config = job.getOrCreateDefaultConfig();
        if (!("new".equals(JSONUtilities.getString(config, "state", null)))) {
            HttpUtilities.respond(response, "error", "Job already started; cannot load more data");
            return;
        }

        ImportingUtilities.loadDataAndPrepareJob(
                request, response, parameters, job, config);
        job.touch();
        job.updating = false;
    }

    private void doUpdateFileSelection(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        com.google.refine.importing.ImportingJob job = com.google.refine.importing.ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        ObjectNode config = job.getOrCreateDefaultConfig();
        if (!("ready".equals(JSONUtilities.getString(config, "state", null)))) {
            HttpUtilities.respond(response, "error", "Job not ready");
            return;
        }

        ArrayNode fileSelectionArray = ParsingUtilities.evaluateJsonStringToArrayNode(
                request.getParameter("fileSelection"));

        com.google.refine.importing.ImportingUtilities.updateJobWithNewFileSelection(job, fileSelectionArray);

        replyWithJobData(request, response, job);
        job.touch();
        job.updating = false;
    }

    private void doUpdateFormatAndOptions(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        com.google.refine.importing.ImportingJob job = com.google.refine.importing.ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        ObjectNode config = job.getOrCreateDefaultConfig();
        if (!("ready".equals(JSONUtilities.getString(config, "state", null)))) {
            HttpUtilities.respond(response, "error", "Job not ready");
            return;
        }

        String format = request.getParameter("format");
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                request.getParameter("options"));

        List<Exception> exceptions = new LinkedList<Exception>();

        com.google.refine.importing.ImportingUtilities.previewParse(job, format, optionObj, exceptions);

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        try {
            writer.writeStartObject();
            if (exceptions.size() == 0) {
                job.project.update(); // update all internal models, indexes, caches, etc.

                writer.writeStringField("status", "ok");
            } else {
                writer.writeStringField("status", "error");
                writer.writeArrayFieldStart("errors");
                writeErrors(writer, exceptions);
                writer.writeEndArray();
            }
            writer.writeEndObject();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            w.flush();
            w.close();
        }
        job.touch();
        job.updating = false;
    }

    private void doInitializeParserUI(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        com.google.refine.importing.ImportingJob job = com.google.refine.importing.ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        String format = request.getParameter("format");
        Format formatRecord = com.google.refine.importing.ImportingManager.formatToRecord.get(format);
        if (formatRecord != null && formatRecord.parser != null) {
            ObjectNode options = formatRecord.parser.createParserUIInitializationData(
                    job, job.getSelectedFileRecords(), format);
            ObjectNode result = ParsingUtilities.mapper.createObjectNode();
            JSONUtilities.safePut(result, "status", "ok");
            JSONUtilities.safePut(result, "options", options);

            Command.respondJSON(response, result);
        } else {
            HttpUtilities.respond(response, "error", "Unrecognized format or format has no parser");
        }
    }

    private void doCreateProject(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        com.google.refine.importing.ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        job.touch();
        ObjectNode config = job.getOrCreateDefaultConfig();
        if (!("ready".equals(JSONUtilities.getString(config, "state", null)))) {
            HttpUtilities.respond(response, "error", "Job not ready");
            return;
        }

        String format = request.getParameter("format");
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                request.getParameter("options"));

        List<Exception> exceptions = new LinkedList<Exception>();

        ImportingUtilities.createProject(job, format, optionObj, exceptions, false);

        HttpUtilities.respond(response, "ok", "done");
    }

    protected static class JobResponse {
        @JsonProperty("code")
        protected String code;
        @JsonProperty("job")
        protected com.google.refine.importing.ImportingJob job;

        protected JobResponse(String code, com.google.refine.importing.ImportingJob job) {
            this.code = code;
            this.job = job;
        }

    }

    /**
     * return the job to the front end.
     *
     * @param request
     * @param response
     * @param job
     * @throws ServletException
     * @throws IOException
     */
    private void replyWithJobData(HttpServletRequest request, HttpServletResponse response, ImportingJob job)
            throws ServletException, IOException {

        Writer w = response.getWriter();
        ParsingUtilities.defaultWriter.writeValue(w, new JobResponse("ok", job));
        w.flush();
        w.close();
    }

}
