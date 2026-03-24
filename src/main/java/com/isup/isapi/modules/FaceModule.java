package com.isup.isapi.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.isup.isapi.IsapiClient;
import com.isup.isapi.IsapiException;
import com.isup.isapi.dto.FaceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;

public class FaceModule {
    private static final Logger log = LoggerFactory.getLogger(FaceModule.class);
    private final IsapiClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public FaceModule(IsapiClient client) {
        this.client = client;
    }

    /**
     * Add or update a face record on the device.
     * Uses multipart POST with JSON metadata + JPEG photo.
     */
    public boolean enrollFace(FaceRecord face) {
        try {
            // Build JSON metadata
            ObjectNode faceInfo = mapper.createObjectNode();
            faceInfo.put("employeeNo", face.getEmployeeNo());
            faceInfo.put("name", face.getName() != null ? face.getName() : "");
            faceInfo.put("userType", "normal");
            faceInfo.put("gender", face.getGender() != null ? face.getGender() : "male");
            faceInfo.set("Valid", mapper.createObjectNode()
                    .put("enable", true)
                    .put("beginTime", "2000-01-01T00:00:00")
                    .put("endTime", "2037-12-31T23:59:59"));

            ObjectNode root = mapper.createObjectNode();
            root.putArray("FaceInfoCond").add(faceInfo);
            String json = mapper.writeValueAsString(root);

            // Decode photo from base64
            byte[] photoBytes = Base64.getDecoder().decode(face.getPhotoBase64());

            // POST multipart
            String result = client.postMultipart(
                    "/ISAPI/Intelligent/FDLib/FDSetUp/picture",
                    json, photoBytes, face.getEmployeeNo() + ".jpg");

            log.info("enrollFace device={} employee={} → {}",
                    client.getBaseUrl(), face.getEmployeeNo(),
                    result.length() > 100 ? result.substring(0, 100) : result);
            return true;
        } catch (IsapiException e) {
            log.error("enrollFace failed for {}: {} (status {})",
                    face.getEmployeeNo(), e.getMessage(), e.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("enrollFace exception for {}: {}", face.getEmployeeNo(), e.getMessage());
            return false;
        }
    }

    /**
     * Delete face by employeeNo from the device.
     * DELETE /ISAPI/Intelligent/FDLib/FDSetUp
     */
    public boolean deleteFace(String employeeNo) {
        try {
            ObjectNode cond = mapper.createObjectNode();
            ObjectNode entry = mapper.createObjectNode();
            entry.put("employeeNo", employeeNo);
            cond.putArray("FaceInfoDelCond").add(entry);

            client.deleteWithBody("/ISAPI/Intelligent/FDLib/FDSetUp",
                    mapper.writeValueAsString(cond));
            log.info("deleteFace employee={} from {}", employeeNo, client.getBaseUrl());
            return true;
        } catch (Exception e) {
            log.warn("deleteFace {} failed: {}", employeeNo, e.getMessage());
            return false;
        }
    }

    /**
     * Search face records on device.
     * POST /ISAPI/Intelligent/FDLib/FDSearch
     */
    public String listFaces(int maxResults) {
        try {
            ObjectNode body = mapper.createObjectNode();
            ObjectNode search = mapper.createObjectNode();
            search.put("searchID", "1");
            search.put("maxResults", maxResults);
            search.put("searchResultPosition", 0);
            body.set("FaceLibSearchCond", search);

            return client.post("/ISAPI/Intelligent/FDLib/FDSearch",
                    mapper.writeValueAsString(body));
        } catch (Exception e) {
            log.warn("listFaces failed: {}", e.getMessage());
            return "{}";
        }
    }
}
