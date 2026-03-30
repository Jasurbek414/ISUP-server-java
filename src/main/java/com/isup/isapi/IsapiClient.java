package com.isup.isapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for Hikvision ISAPI endpoints.
 * Handles Digest/Basic authentication automatically.
 * Base URL: http://DEVICE_IP/ISAPI/
 */
public class IsapiClient {

    private static final Logger log = LoggerFactory.getLogger(IsapiClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType XML  = MediaType.parse("application/xml; charset=utf-8");

    private final OkHttpClient http;
    private final String baseUrl;
    protected final ObjectMapper mapper;

    public IsapiClient(String deviceIp, int port, boolean useHttps, String username, String password) {
        String proto = useHttps ? "https://" : "http://";
        this.baseUrl = proto + deviceIp + (port == 80 && !useHttps ? "" : (port == 443 && useHttps ? "" : ":" + port));
        this.mapper  = new ObjectMapper();
        this.http = new OkHttpClient.Builder()
                .authenticator(new IsapiAuth(username, password))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public IsapiClient(String deviceIp, String username, String password) {
        this(deviceIp, 80, false, username, password);
    }

    /** GET request, returns response body as String. */
    public String get(String path) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .build();
        return executeAndGet(request);
    }

    /** PUT with JSON body. */
    public String put(String path, String jsonBody) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .put(RequestBody.create(jsonBody, JSON))
                .build();
        return executeAndGet(request);
    }

    /** POST with JSON body. */
    public String post(String path, String jsonBody) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(jsonBody, JSON))
                .build();
        return executeAndGet(request);
    }

    /** DELETE request. */
    public String delete(String path) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .delete()
                .build();
        return executeAndGet(request);
    }

    /** DELETE with JSON body. */
    public String deleteWithBody(String path, String jsonBody) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .delete(RequestBody.create(jsonBody, JSON))
                .build();
        return executeAndGet(request);
    }

    /** Multipart POST (for face photo upload). */
    public String postMultipart(String path, String jsonPart, byte[] photoPart, String photoFilename) throws IOException {
        RequestBody multipart = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("FaceDataRecord", null,
                        RequestBody.create(jsonPart, JSON))
                .addFormDataPart("FaceImage", photoFilename,
                        RequestBody.create(photoPart, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(multipart)
                .build();
        return executeAndGet(request);
    }

    private String executeAndGet(Request request) throws IOException {
        try (Response response = http.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.warn("ISAPI {} {} → {}: {}", request.method(), request.url(), response.code(), body.length() > 200 ? body.substring(0, 200) : body);
                throw new IsapiException("ISAPI error " + response.code() + ": " + body, response.code());
            }
            log.debug("ISAPI {} {} → {}", request.method(), request.url(), response.code());
            return body;
        }
    }

    public String getBaseUrl() { return baseUrl; }
}
