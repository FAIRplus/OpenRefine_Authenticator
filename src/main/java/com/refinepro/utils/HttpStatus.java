package com.refinepro.utils;

public class HttpStatus {

    private static final String[][] REASON_PHRASES = new String[][]{new String[0], new String[3], new String[8], new String[8], new String[25], new String[8]};


    static {
        addStatusCodeMap(200, "OK");
        addStatusCodeMap(201, "Created");
        addStatusCodeMap(202, "Accepted");
        addStatusCodeMap(204, "No Content");
        addStatusCodeMap(301, "Moved Permanently");
        addStatusCodeMap(302, "Moved Temporarily");
        addStatusCodeMap(304, "Not Modified");
        addStatusCodeMap(400, "Bad Request");
        addStatusCodeMap(401, "Unauthorized");
        addStatusCodeMap(403, "Forbidden");
        addStatusCodeMap(404, "Not Found");
        addStatusCodeMap(500, "Internal Server Error");
        addStatusCodeMap(501, "Not Implemented");
        addStatusCodeMap(502, "Bad Gateway");
        addStatusCodeMap(503, "Service Unavailable");
        addStatusCodeMap(100, "Continue");
        addStatusCodeMap(307, "Temporary Redirect");
        addStatusCodeMap(405, "Method Not Allowed");
        addStatusCodeMap(409, "Conflict");
        addStatusCodeMap(412, "Precondition Failed");
        addStatusCodeMap(413, "Request Too Long");
        addStatusCodeMap(414, "Request-URI Too Long");
        addStatusCodeMap(415, "Unsupported Media Type");
        addStatusCodeMap(300, "Multiple Choices");
        addStatusCodeMap(303, "See Other");
        addStatusCodeMap(305, "Use Proxy");
        addStatusCodeMap(402, "Payment Required");
        addStatusCodeMap(406, "Not Acceptable");
        addStatusCodeMap(407, "Proxy Authentication Required");
        addStatusCodeMap(408, "Request Timeout");
        addStatusCodeMap(101, "Switching Protocols");
        addStatusCodeMap(203, "Non Authoritative Information");
        addStatusCodeMap(205, "Reset Content");
        addStatusCodeMap(206, "Partial Content");
        addStatusCodeMap(504, "Gateway Timeout");
        addStatusCodeMap(505, "Http Version Not Supported");
        addStatusCodeMap(410, "Gone");
        addStatusCodeMap(411, "Length Required");
        addStatusCodeMap(416, "Requested Range Not Satisfiable");
        addStatusCodeMap(417, "Expectation Failed");
        addStatusCodeMap(102, "Processing");
        addStatusCodeMap(207, "Multi-Status");
        addStatusCodeMap(422, "Unprocessable Entity");
        addStatusCodeMap(419, "Insufficient Space On Resource");
        addStatusCodeMap(420, "Method Failure");
        addStatusCodeMap(423, "Locked");
        addStatusCodeMap(507, "Insufficient Storage");
        addStatusCodeMap(424, "Failed Dependency");
    }
    private static void addStatusCodeMap(int statusCode, String reasonPhrase) {
        int classIndex = statusCode / 100;
        REASON_PHRASES[classIndex][statusCode - classIndex * 100] = reasonPhrase;
    }

    public static String getStatusText(int statusCode) {
        if (statusCode < 0) {
            throw new IllegalArgumentException("status code may not be negative");
        } else {
            int classIndex = statusCode / 100;
            int codeIndex = statusCode - classIndex * 100;
            return classIndex >= 1 && classIndex <= REASON_PHRASES.length - 1 && codeIndex >= 0 && codeIndex <= REASON_PHRASES[classIndex].length - 1 ? REASON_PHRASES[classIndex][codeIndex] : null;
        }
    }
}
