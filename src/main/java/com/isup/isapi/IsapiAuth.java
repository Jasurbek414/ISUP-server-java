package com.isup.isapi;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hikvision Digest Authentication handler for OkHttp.
 * Handles both Basic and Digest auth challenges from ISAPI endpoints.
 */
public class IsapiAuth implements Authenticator {

    private static final Logger log = LoggerFactory.getLogger(IsapiAuth.class);

    private final String username;
    private final String password;

    public IsapiAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (response.request().header("Authorization") != null) {
            return null; // Already tried auth, give up
        }

        String wwwAuth = response.header("WWW-Authenticate");
        if (wwwAuth == null) return null;

        if (wwwAuth.startsWith("Digest")) {
            return buildDigestAuth(response.request(), wwwAuth);
        } else if (wwwAuth.startsWith("Basic")) {
            String credential = Credentials.basic(username, password);
            return response.request().newBuilder()
                    .header("Authorization", credential)
                    .build();
        }
        return null;
    }

    private Request buildDigestAuth(Request request, String wwwAuth) {
        try {
            String realm  = extractParam(wwwAuth, "realm");
            String nonce  = extractParam(wwwAuth, "nonce");
            String opaque = extractParam(wwwAuth, "opaque");
            String qop    = extractParam(wwwAuth, "qop");

            String method = request.method();
            String uri    = request.url().encodedPath();
            if (request.url().encodedQuery() != null) {
                uri += "?" + request.url().encodedQuery();
            }

            String ha1 = md5(username + ":" + realm + ":" + password);
            String ha2 = md5(method + ":" + uri);

            String nc     = "00000001";
            String cnonce = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            String responseHash;
            if ("auth".equals(qop)) {
                responseHash = md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);
            } else {
                responseHash = md5(ha1 + ":" + nonce + ":" + ha2);
            }

            StringBuilder auth = new StringBuilder("Digest ");
            auth.append("username=\"").append(username).append("\", ");
            auth.append("realm=\"").append(realm).append("\", ");
            auth.append("nonce=\"").append(nonce).append("\", ");
            auth.append("uri=\"").append(uri).append("\", ");
            if (qop != null) {
                auth.append("qop=").append(qop).append(", ");
                auth.append("nc=").append(nc).append(", ");
                auth.append("cnonce=\"").append(cnonce).append("\", ");
            }
            auth.append("response=\"").append(responseHash).append("\"");
            if (opaque != null) {
                auth.append(", opaque=\"").append(opaque).append("\"");
            }

            return request.newBuilder()
                    .header("Authorization", auth.toString())
                    .build();
        } catch (Exception e) {
            log.warn("Digest auth build failed: {}", e.getMessage());
            return null;
        }
    }

    private static String extractParam(String header, String param) {
        Pattern p = Pattern.compile(param + "=\"?([^,\"]+)\"?");
        Matcher m = p.matcher(header);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
