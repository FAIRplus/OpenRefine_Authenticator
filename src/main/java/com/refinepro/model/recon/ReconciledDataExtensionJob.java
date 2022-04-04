/*
    Custom code from original OpenRefine project main/src/com/google/refine/model/recon/ReconciledDataExtensionJob.java
*/

package com.refinepro.model.recon;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.expr.functions.ToDate;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.ReconType;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.JsonViews;
import com.google.refine.util.ParsingUtilities;
import com.refinepro.importing.HttpClient;


public class ReconciledDataExtensionJob extends com.google.refine.model.recon.ReconciledDataExtensionJob {


    static public class DataExtensionProperty {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        @JsonView(JsonViews.NonSaveMode.class)
        public final String name;
        @JsonProperty("settings")
        @JsonInclude(Include.NON_NULL)
        public final Map<String, Object> settings;

        @JsonCreator
        public DataExtensionProperty(
                @JsonProperty("id")
                        String id,
                @JsonProperty("name")
                        String name,
                @JsonProperty("settings")
                        Map<String, Object> settings) {
            this.id = id;
            this.name = name;
            this.settings = settings;
        }
    }

    // Json serialization is used in PreviewExtendDataCommand
    static public class ColumnInfo {
        @JsonProperty("name")
        final public String name;
        @JsonProperty("id")
        final public String id;
        final public ReconType expectedType;

        @JsonCreator
        protected ColumnInfo(
                @JsonProperty("name")
                        String name,
                @JsonProperty("id")
                        String id,
                @JsonProperty("type")
                        ReconType expectedType) {
            this.name = name;
            this.id = id;
            this.expectedType = expectedType;
        }
    }

    final public DataExtensionConfig extension;
    final public String endpoint;
    public String authenticatorHeader = "";
    public String authenticatorToken = "";
    final public List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

    // not final: initialized lazily
    private static HttpClient httpClient = null;

    public ReconciledDataExtensionJob(DataExtensionConfig obj, String endpoint) {
        super(obj, endpoint);
        this.extension = obj;
        this.endpoint = endpoint;
    }

    /**
     * @todo Although the HTTP code has been unified, there may still be opportunity
     * to refactor a higher level querying library out of this which could be shared
     * with StandardReconConfig
     * <p>
     * It may also be possible to extract a library to query reconciliation services
     * which could be used outside of OpenRefine.
     */
    public Map<String, DataExtension> extend(
            Set<String> ids,
            Map<String, ReconCandidate> reconCandidateMap
    ) throws Exception {
        StringWriter writer = new StringWriter();
        formulateQuery(ids, extension, writer);

        String query = writer.toString();
        String response = postExtendQuery(this.endpoint, query, this.authenticatorHeader, this.authenticatorToken);

        ObjectNode o = ParsingUtilities.mapper.readValue(response, ObjectNode.class);

        if (columns.size() == 0) {
            // Extract the column metadata
            List<ColumnInfo> newColumns = ParsingUtilities.mapper.convertValue(o.get("meta"), new TypeReference<List<ColumnInfo>>() {
            });
            columns.addAll(newColumns);
        }

        Map<String, DataExtension> map = new HashMap<String, DataExtension>();
        if (o.has("rows") && o.get("rows") instanceof ObjectNode) {
            ObjectNode records = (ObjectNode) o.get("rows");

            // for each identifier
            for (String id : ids) {
                if (records.has(id) && records.get(id) instanceof ObjectNode) {
                    ObjectNode record = (ObjectNode) records.get(id);

                    DataExtension ext = collectResult(record, reconCandidateMap);

                    if (ext != null) {
                        map.put(id, ext);
                    }
                }
            }
        }

        return map;
    }

    static protected String postExtendQuery(String endpoint, String query, String authenticatorHeader, String authenticatorToken) throws IOException {

        return getHttpClient().postNameValue(endpoint, "extend", query, authenticatorHeader, authenticatorToken);
    }

    private static HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new HttpClient();
        }
        return httpClient;
    }

    protected DataExtension collectResult(
            ObjectNode record,
            Map<String, ReconCandidate> reconCandidateMap
    ) {
        List<Object[]> rows = new ArrayList<Object[]>();

        // for each property
        int colindex = 0;
        for (ColumnInfo ci : columns) {
            String pid = ci.id;
            ArrayNode values = JSONUtilities.getArray(record, pid);
            if (values == null) {
                continue;
            }

            // for each value
            for (int rowindex = 0; rowindex < values.size(); rowindex++) {
                if (!(values.get(rowindex) instanceof ObjectNode)) {
                    continue;
                }
                ObjectNode val = (ObjectNode) values.get(rowindex);
                // store a reconciled value
                if (val.has("id")) {
                    storeCell(rows, rowindex, colindex, val, reconCandidateMap);
                } else if (val.has("str")) {
                    // store a bare string
                    String str = val.get("str").asText();
                    storeCell(rows, rowindex, colindex, str);
                } else if (val.has("float")) {
                    double v = val.get("float").asDouble();
                    storeCell(rows, rowindex, colindex, v);
                } else if (val.has("int")) {
                    int v = val.get("int").asInt();
                    storeCell(rows, rowindex, colindex, v);
                } else if (val.has("date")) {
                    ToDate td = new ToDate();
                    String[] args = new String[1];
                    args[0] = val.get("date").asText();
                    Object v = td.call(null, args);
                    storeCell(rows, rowindex, colindex, v);
                } else if (val.has("bool")) {
                    boolean v = val.get("bool").asBoolean();
                    storeCell(rows, rowindex, colindex, v);
                }
            }
            colindex++;
        }


        Object[][] data = new Object[rows.size()][columns.size()];
        rows.toArray(data);

        return new DataExtension(data);
    }

    protected void storeCell(
            List<Object[]> rows,
            int row,
            int col,
            Object value
    ) {
        while (row >= rows.size()) {
            rows.add(new Object[columns.size()]);
        }
        rows.get(row)[col] = value;
    }

    protected void storeCell(
            List<Object[]> rows,
            int row,
            int col,
            ObjectNode obj,
            Map<String, ReconCandidate> reconCandidateMap
    ) {
        String id = obj.get("id").asText();
        ReconCandidate rc;
        if (reconCandidateMap.containsKey(id)) {
            rc = reconCandidateMap.get(id);
        } else {
            rc = new ReconCandidate(
                    obj.get("id").asText(),
                    obj.get("name").asText(),
                    JSONUtilities.getStringArray(obj, "type"),
                    100
            );

            reconCandidateMap.put(id, rc);
        }

        storeCell(rows, row, col, rc);
    }


    static protected void formulateQuery(Set<String> ids, DataExtensionConfig node, Writer writer) throws IOException {
        DataExtensionQuery query = new DataExtensionQuery(ids.stream().filter(e -> e != null).collect(Collectors.toList()), node.properties);
        ParsingUtilities.saveWriter.writeValue(writer, query);
    }
}
