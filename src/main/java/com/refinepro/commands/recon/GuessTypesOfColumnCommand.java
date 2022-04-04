/*
    Custom code from original OpenRefine project main/src/com/google/refine/commands/recon/GuessTypesOfColumnCommand.java
*/
package com.refinepro.commands.recon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.commands.Command;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.ReconType;
import com.google.refine.model.Row;
import com.google.refine.model.recon.StandardReconConfig.ReconResult;
import com.google.refine.util.ParsingUtilities;
import com.refinepro.importing.HttpClient;

public class GuessTypesOfColumnCommand extends Command {
    
    final static int DEFAULT_SAMPLE_SIZE = 10;
    private int sampleSize = DEFAULT_SAMPLE_SIZE;
     
    protected static class TypesResponse {
        @JsonProperty("code")
        protected String code;
        @JsonProperty("message")
        @JsonInclude(Include.NON_NULL)
        protected String message;
        @JsonProperty("types")
        @JsonInclude(Include.NON_NULL)
        List<TypeGroup> types;
        
        protected TypesResponse(
                String code,
                String message,
                List<TypeGroup> types) {
            this.code = code;
            this.message = message;
            this.types = types;
        }
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	if(!hasValidCSRFToken(request)) {
    		respondCSRFError(response);
    		return;
    	}
        
        try {
            Project project = getProject(request);
            String columnName = request.getParameter("columnName");
            String serviceUrl = request.getParameter("service");
            
            Column column = project.columnModel.getColumnByName(columnName);
            if (column == null) {
                respondJSON(response, new TypesResponse("error", "No such column", null));
            } else {
                List<TypeGroup> typeGroups = guessTypes(project, column, serviceUrl, request);
                respondJSON(response, new TypesResponse("ok", null, typeGroups));   
            }

        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
    protected static class IndividualQuery {
        @JsonProperty("query")
        protected String query;
        @JsonProperty("limit")
        protected int limit;
        
        protected IndividualQuery(String query, int limit) {
            this.query = query;
            this.limit = limit;
        }
    }
    
    /**
     * Run relevance searches for the first n cells in the given column and
     * count the types of the results. Return a sorted list of types, from most
     * frequent to least. 
     * 
     * @param project
     * @param column
     * @param request
     * @return
     * @throws IOException
     */
    protected List<TypeGroup> guessTypes(Project project, Column column, String serviceUrl, HttpServletRequest request)
            throws IOException {
        Map<String, TypeGroup> map = new HashMap<String, TypeGroup>();
        
        int cellIndex = column.getCellIndex();
        
        List<String> samples = new ArrayList<String>(sampleSize);
        Set<String> sampleSet = new HashSet<String>();
        
        for (Row row : project.rows) {
            Object value = row.getCellValue(cellIndex);
            if (ExpressionUtils.isNonBlankData(value)) {
                String s = value.toString().trim();
                if (!sampleSet.contains(s)) {
                    samples.add(s);
                    sampleSet.add(s);
                    if (samples.size() >= sampleSize) {
                        break;
                    }
                }
            }
        }
        
        Map<String, IndividualQuery> queryMap = new HashMap<>();
        for (int i = 0; i < samples.size(); i++) {
            queryMap.put("q" + i, new IndividualQuery(samples.get(i), 3));
        }
        
        String queriesString = ParsingUtilities.defaultWriter.writeValueAsString(queryMap);
        String responseString;
        try {
            responseString = postQueries(serviceUrl, queriesString, request);
            ObjectNode o = ParsingUtilities.evaluateJsonStringToObjectNode(responseString);

            Iterator<JsonNode> iterator = o.iterator();
            while (iterator.hasNext()) {
                JsonNode o2 = iterator.next();
                if (!(o2.has("result") && o2.get("result") instanceof ArrayNode)) {
                    continue;
                }

                ArrayNode results = (ArrayNode) o2.get("result");
                List<ReconResult> reconResults = ParsingUtilities.mapper.convertValue(results, new TypeReference<List<ReconResult>>() {});
                int count = reconResults.size();

                for (int j = 0; j < count; j++) {
                    ReconResult result = reconResults.get(j);
                    double score = 1.0 / (1 + j); // score by each result's rank

                    List<ReconType> types = result.types;
                    int typeCount = types.size();

                    for (int t = 0; t < typeCount; t++) {
                        ReconType type = types.get(t);
                        double score2 = score * (typeCount - t) / typeCount;
                        if (map.containsKey(type.id)) {
                            TypeGroup tg = map.get(type.id);
                            tg.score += score2;
                            tg.count++;
                        } else {
                            map.put(type.id, new TypeGroup(type.id, type.name, score2));
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to guess cell types for load\n" + queriesString, e);
            throw e;
        }

        List<TypeGroup> types = new ArrayList<TypeGroup>(map.values());
        Collections.sort(types, new Comparator<TypeGroup>() {
            @Override
            public int compare(TypeGroup o1, TypeGroup o2) {
                int c = Math.min(sampleSize, o2.count) - Math.min(sampleSize, o1.count);
                if (c != 0) {
                    return c;
                }
                return (int) Math.signum(o2.score / o2.count - o1.score / o1.count);
            }
        });
        
        return types;
    }

    private String postQueries(String serviceUrl, String queriesString, HttpServletRequest request) throws IOException {
        String authHeader = request.getParameter("authenticatorHeader");
        String authToken = request.getParameter("authenticatorToken");
        HttpClient client = new HttpClient();
        return client.postNameValue(serviceUrl, "queries", queriesString, authHeader, authToken);
    }

    static protected class TypeGroup {
        @JsonProperty("id")
        protected String id;
        @JsonProperty("name")
        protected String name;
        @JsonProperty("count")
        protected int count;
        @JsonProperty("score")
        protected double score;
        
        TypeGroup(String id, String name, double score) {
            this.id = id;
            this.name = name;
            this.score = score;
            this.count = 1;
        }
    }
    
    // for testability
    protected void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }
}
