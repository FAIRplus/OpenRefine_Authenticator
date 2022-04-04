package com.refinepro.importing;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.importing.FormatGuesser;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.importing.UrlRewriter;
import com.google.refine.importing.UrlRewriter.Result;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.message.BasicHeader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.*;

/*
    Custom code from original OpenRefine project main/src/com/google/refine/importing/ImportingUtilities.java
*/
public class ImportingUtilities extends com.google.refine.importing.ImportingUtilities {

    public static void loadDataAndPrepareJob(HttpServletRequest request, HttpServletResponse response, Properties parameters, final ImportingJob job, ObjectNode config) throws IOException, ServletException {
        ObjectNode retrievalRecord = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(config, "retrievalRecord", retrievalRecord);
        JSONUtilities.safePut(config, "state", "loading-raw-data");
        final ObjectNode progress = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(config, "progress", progress);

        try {
            retrieveContentFromPostRequest(request, parameters, job.getRawDataDir(), retrievalRecord, new com.google.refine.importing.ImportingUtilities.Progress() {
                public void setProgress(String message, int percent) {
                    if (message != null) {
                        JSONUtilities.safePut(progress, "message", message);
                    }

                    JSONUtilities.safePut(progress, "percent", (long) percent);
                }

                public boolean isCanceled() {
                    return job.canceled;
                }
            });
        } catch (Exception var10) {
            JSONUtilities.safePut(config, "state", "error");
            JSONUtilities.safePut(config, "error", "Error uploading data");
            JSONUtilities.safePut(config, "errorDetails", var10.getLocalizedMessage());
            return;
        }

        ArrayNode fileSelectionIndexes = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.safePut(config, "fileSelection", fileSelectionIndexes);
        String bestFormat = autoSelectFiles(job, retrievalRecord, fileSelectionIndexes);
        bestFormat = guessBetterFormat(job, bestFormat);
        ArrayNode rankedFormats = ParsingUtilities.mapper.createArrayNode();
        rankFormats(job, bestFormat, rankedFormats);
        JSONUtilities.safePut(config, "rankedFormats", rankedFormats);
        JSONUtilities.safePut(config, "state", "ready");
        JSONUtilities.safePut(config, "hasData", true);
        config.remove("progress");
    }

    static public void retrieveContentFromPostRequest(
            HttpServletRequest request,
            Properties parameters,
            File rawDataDir,
            ObjectNode retrievalRecord,
            final Progress progress
    ) throws Exception {
        ArrayNode fileRecords = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.safePut(retrievalRecord, "files", fileRecords);

        int clipboardCount = 0;
        int uploadCount = 0;
        int downloadCount = 0;
        int archiveCount = 0;

        // This tracks the total progress, which involves uploading data from the client
        // as well as downloading data from URLs.
        final SavingUpdate update = new SavingUpdate() {
            @Override
            public void savedMore() {
                progress.setProgress(null, calculateProgressPercent(totalExpectedSize, totalRetrievedSize));
            }

            @Override
            public boolean isCanceled() {
                return progress.isCanceled();
            }
        };

        DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();

        ServletFileUpload upload = new ServletFileUpload(fileItemFactory);
        upload.setProgressListener(new ProgressListener() {
            boolean setContentLength = false;
            long lastBytesRead = 0;

            @Override
            public void update(long bytesRead, long contentLength, int itemCount) {
                if (!setContentLength) {
                    // Only try to set the content length if we really know it.
                    if (contentLength >= 0) {
                        update.totalExpectedSize += contentLength;
                        setContentLength = true;
                    }
                }
                if (setContentLength) {
                    update.totalRetrievedSize += (bytesRead - lastBytesRead);
                    lastBytesRead = bytesRead;

                    update.savedMore();
                }
            }
        });

        List<FileItem> tempFiles = (List<FileItem>) upload.parseRequest(request);

        progress.setProgress("Uploading data ...", -1);
        parts:
        for (FileItem fileItem : tempFiles) {
            if (progress.isCanceled()) {
                break;
            }

            InputStream stream = fileItem.getInputStream();

            String name = fileItem.getFieldName().toLowerCase();
            if (fileItem.isFormField()) {
                if (name.equals("clipboard")) {
                    String encoding = request.getCharacterEncoding();
                    if (encoding == null) {
                        encoding = "UTF-8";
                    }

                    File file = allocateFile(rawDataDir, "clipboard.txt");

                    ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
                    JSONUtilities.safePut(fileRecord, "origin", "clipboard");
                    JSONUtilities.safePut(fileRecord, "declaredEncoding", encoding);
                    JSONUtilities.safePut(fileRecord, "declaredMimeType", (String) null);
                    JSONUtilities.safePut(fileRecord, "format", "text");
                    JSONUtilities.safePut(fileRecord, "fileName", "(clipboard)");
                    JSONUtilities.safePut(fileRecord, "location", getRelativePath(file, rawDataDir));

                    progress.setProgress("Uploading pasted clipboard text",
                            calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));

                    JSONUtilities.safePut(fileRecord, "size", saveStreamToFiles(stream, file, null));
                    JSONUtilities.append(fileRecords, fileRecord);

                    clipboardCount++;

                } else if (name.equals("download")) {
                    String urlString = Streams.asString(stream);
                    URL url = new URL(urlString);

                    ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
                    JSONUtilities.safePut(fileRecord, "origin", "download");
                    JSONUtilities.safePut(fileRecord, "url", urlString);

                    for (UrlRewriter rewriter : ImportingManager.urlRewriters) {
                        Result result = rewriter.rewrite(urlString);
                        if (result != null) {
                            urlString = result.rewrittenUrl;
                            url = new URL(urlString);

                            JSONUtilities.safePut(fileRecord, "url", urlString);
                            JSONUtilities.safePut(fileRecord, "format", result.format);
                            if (!result.download) {
                                downloadCount++;
                                JSONUtilities.append(fileRecords, fileRecord);
                                continue parts;
                            }
                        }
                    }

                    if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                        final URL lastUrl = url;
                        final HttpClientResponseHandler<String> responseHandler = new HttpClientResponseHandler<String>() {

                            @Override
                            public String handleResponse(final ClassicHttpResponse response) throws IOException {
                                final int status = response.getCode();
                                if (status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION) {
                                    final HttpEntity entity = response.getEntity();
                                    if (entity == null) {
                                        throw new IOException("No content found in " + lastUrl.toExternalForm());
                                    }

                                    try {
                                        InputStream stream2 = entity.getContent();

                                        String mimeType = null;
                                        String charset = null;
                                        ContentType contentType = ContentType.parse(entity.getContentType());
                                        if (contentType != null) {
                                            mimeType = contentType.getMimeType();
                                            Charset cs = contentType.getCharset();
                                            if (cs != null) {
                                                charset = cs.toString();
                                            }
                                        }
                                        JSONUtilities.safePut(fileRecord, "declaredMimeType", mimeType);
                                        JSONUtilities.safePut(fileRecord, "declaredEncoding", charset);
                                        if (saveStream(stream2, lastUrl, rawDataDir, progress, update,
                                                fileRecord, fileRecords,
                                                entity.getContentLength())) {
                                            return "saved"; // signal to increment archive count
                                        }

                                    } catch (final IOException ex) {
                                        throw new ClientProtocolException(ex);
                                    }
                                    return null;
                                } else {
                                    // String errorBody = EntityUtils.toString(response.getEntity());
                                    throw new ClientProtocolException(String.format("HTTP error %d : %s for URL %s", status,
                                            response.getReasonPhrase(), lastUrl.toExternalForm()));
                                }
                            }
                        };

                        HttpClient httpClient = new HttpClient();
                        Header[] headers = new Header[1];
                        String authHeader = parameters.getProperty("authenticatorHeader", "");
                        String authToken = parameters.getProperty("authenticatorToken", "");
                        headers[0] = new BasicHeader(authHeader, authToken);
                        if (httpClient.getResponse(urlString, headers, responseHandler) != null) {
                            archiveCount++;
                        }
                        ;
                        downloadCount++;
                    } else {
                        // Fallback handling for non HTTP connections (only FTP?)
                        URLConnection urlConnection = url.openConnection();
                        urlConnection.setConnectTimeout(5000);
                        urlConnection.connect();
                        InputStream stream2 = urlConnection.getInputStream();
                        JSONUtilities.safePut(fileRecord, "declaredEncoding",
                                urlConnection.getContentEncoding());
                        JSONUtilities.safePut(fileRecord, "declaredMimeType",
                                urlConnection.getContentType());
                        try {
                            if (saveStream(stream2, url, rawDataDir, progress,
                                    update, fileRecord, fileRecords,
                                    urlConnection.getContentLength())) {
                                archiveCount++;
                            }
                            downloadCount++;
                        } finally {
                            stream2.close();
                        }
                    }
                } else {
                    String value = Streams.asString(stream);
                    parameters.put(name, value);
                    // TODO: We really want to store this on the request so it's available for everyone
//                    request.getParameterMap().put(name, value);
                }

            } else { // is file content
                String fileName = fileItem.getName();
                if (fileName.length() > 0) {
                    long fileSize = fileItem.getSize();

                    File file = allocateFile(rawDataDir, fileName);

                    ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
                    JSONUtilities.safePut(fileRecord, "origin", "upload");
                    JSONUtilities.safePut(fileRecord, "declaredEncoding", request.getCharacterEncoding());
                    JSONUtilities.safePut(fileRecord, "declaredMimeType", fileItem.getContentType());
                    JSONUtilities.safePut(fileRecord, "fileName", fileName);
                    JSONUtilities.safePut(fileRecord, "location", getRelativePath(file, rawDataDir));

                    progress.setProgress(
                            "Saving file " + fileName + " locally (" + formatBytes(fileSize) + " bytes)",
                            calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));

                    JSONUtilities.safePut(fileRecord, "size", saveStreamToFiles(stream, file, null));
                    // TODO: This needs to be refactored to be able to test import from archives
                    if (postProcessRetrievedFile(rawDataDir, file, fileRecord, fileRecords, progress)) {
                        archiveCount++;
                    }

                    uploadCount++;
                }
            }

            stream.close();
        }

        // Delete all temp files.
        for (FileItem fileItem : tempFiles) {
            fileItem.delete();
        }

        JSONUtilities.safePut(retrievalRecord, "uploadCount", uploadCount);
        JSONUtilities.safePut(retrievalRecord, "downloadCount", downloadCount);
        JSONUtilities.safePut(retrievalRecord, "clipboardCount", clipboardCount);
        JSONUtilities.safePut(retrievalRecord, "archiveCount", archiveCount);
    }

    private static boolean saveStream(InputStream stream, URL url, File rawDataDir, final Progress progress,
                                      final SavingUpdate update, ObjectNode fileRecord, ArrayNode fileRecords, long length)
            throws IOException {
        String localname = url.getPath();
        if (localname.isEmpty() || localname.endsWith("/")) {
            localname = localname + "temp";
        }
        File file = allocateFile(rawDataDir, localname);

        JSONUtilities.safePut(fileRecord, "fileName", file.getName());
        JSONUtilities.safePut(fileRecord, "location", getRelativePath(file, rawDataDir));

        update.totalExpectedSize += length;

        progress.setProgress("Downloading " + url.toString(),
                calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));

        long actualLength = saveStreamToFiles(stream, file, update);
        JSONUtilities.safePut(fileRecord, "size", actualLength);
        if (actualLength == 0) {
            throw new IOException("No content found in " + url.toString());
        } else if (length >= 0) {
            update.totalExpectedSize += (actualLength - length);
        } else {
            update.totalExpectedSize += actualLength;
        }
        progress.setProgress("Saving " + url.toString() + " locally",
                calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));
        return postProcessRetrievedFile(rawDataDir, file, fileRecord, fileRecords, progress);
    }


    static private abstract class SavingUpdate {
        public long totalExpectedSize = 0;
        public long totalRetrievedSize = 0;

        abstract public void savedMore();

        abstract public boolean isCanceled();
    }

    static public long saveStreamToFiles(InputStream stream, File file, SavingUpdate update) throws IOException {
        long length = 0;
        FileOutputStream fos = new FileOutputStream(file);
        try {
            byte[] bytes = new byte[16 * 1024];
            int c;
            while ((update == null || !update.isCanceled()) && (c = stream.read(bytes)) > 0) {
                fos.write(bytes, 0, c);
                length += c;

                if (update != null) {
                    update.totalRetrievedSize += c;
                    update.savedMore();
                }
            }
            return length;
        } finally {
            fos.close();
        }
    }


    static private int calculateProgressPercent(long totalExpectedSize, long totalRetrievedSize) {
        return totalExpectedSize == 0 ? -1 : (int) (totalRetrievedSize * 100 / totalExpectedSize);
    }

    static private String formatBytes(long bytes) {
        return NumberFormat.getIntegerInstance().format(bytes);
    }

    static String guessBetterFormat(ImportingJob job, String bestFormat) {
        ObjectNode retrievalRecord = job.getRetrievalRecord();
        return retrievalRecord != null ? guessBetterFormat(job, retrievalRecord, bestFormat) : bestFormat;
    }

    static String guessBetterFormat(ImportingJob job, ObjectNode retrievalRecord, String bestFormat) {
        ArrayNode fileRecords = JSONUtilities.getArray(retrievalRecord, "files");
        return fileRecords != null ? guessBetterFormat(job, fileRecords, bestFormat) : bestFormat;
    }

    static String guessBetterFormat(ImportingJob job, ArrayNode fileRecords, String bestFormat) {
        if (bestFormat != null && fileRecords != null && fileRecords.size() > 0) {
            ObjectNode firstFileRecord = JSONUtilities.getObjectElement(fileRecords, 0);
            String encoding = getEncoding(firstFileRecord);
            String location = JSONUtilities.getString(firstFileRecord, "location", (String) null);
            if (location != null) {
                File file = new File(job.getRawDataDir(), location);

                while (true) {
                    String betterFormat = null;
                    List<FormatGuesser> guessers = (List) ImportingManager.formatToGuessers.get(bestFormat);
                    if (guessers != null) {
                        Iterator var9 = guessers.iterator();

                        while (var9.hasNext()) {
                            FormatGuesser guesser = (FormatGuesser) var9.next();
                            betterFormat = guesser.guess(file, encoding, bestFormat);
                            if (betterFormat != null) {
                                break;
                            }
                        }
                    }

                    if (betterFormat == null || betterFormat.equals(bestFormat)) {
                        break;
                    }

                    bestFormat = betterFormat;
                }
            }
        }

        return bestFormat;
    }

    static void rankFormats(ImportingJob job, final String bestFormat, ArrayNode rankedFormats) {
        final Map<String, String[]> formatToSegments = new HashMap();
        boolean download = bestFormat == null ? true : ((ImportingManager.Format) ImportingManager.formatToRecord.get(bestFormat)).download;
        List<String> formats = new ArrayList(ImportingManager.formatToRecord.keySet().size());
        Iterator var6 = ImportingManager.formatToRecord.keySet().iterator();

        String format;
        while (var6.hasNext()) {
            format = (String) var6.next();
            ImportingManager.Format record = (ImportingManager.Format) ImportingManager.formatToRecord.get(format);
            if (record.uiClass != null && record.parser != null && record.download == download) {
                formats.add(format);
                formatToSegments.put(format, format.split("/"));
            }
        }

        if (bestFormat == null) {
            Collections.sort(formats);
        } else {
            Collections.sort(formats, new Comparator<String>() {
                public int compare(String format1, String format2) {
                    if (format1.equals(bestFormat)) {
                        return -1;
                    } else {
                        return format2.equals(bestFormat) ? 1 : this.compareBySegments(format1, format2);
                    }
                }

                int compareBySegments(String format1, String format2) {
                    int c = this.commonSegments(format2) - this.commonSegments(format1);
                    return c != 0 ? c : format1.compareTo(format2);
                }

                int commonSegments(String format) {
                    String[] bestSegments = (String[]) formatToSegments.get(bestFormat);
                    String[] segments = (String[]) formatToSegments.get(format);
                    if (bestSegments != null && segments != null) {
                        int i;
                        for (i = 0; i < bestSegments.length && i < segments.length && bestSegments[i].equals(segments[i]); ++i) {
                        }

                        return i;
                    } else {
                        return 0;
                    }
                }
            });
        }

        var6 = formats.iterator();

        while (var6.hasNext()) {
            format = (String) var6.next();
            rankedFormats.add(format);
        }

    }
}
