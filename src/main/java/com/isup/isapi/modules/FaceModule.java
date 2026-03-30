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
     * Supports both Access Control terminals (FaceInfo/SetUp) and NVRs (FDLib).
     */
    public boolean enrollFace(FaceRecord face) {
        // Many face terminals use /ISAPI/AccessControl/FaceInfo/SetUp?format=json
        // While cameras/NVRs use /ISAPI/Intelligent/FDLib/FDSetUp/picture
        
        try {
            // Build metadata
            String json;
            String endpoint;
            
            // Heuristic or capability check could be here. For now, try AccessControl first if it's a Face Terminal
            ObjectNode root = mapper.createObjectNode();
            ObjectNode faceInfo = mapper.createObjectNode();
            faceInfo.put("employeeNo", face.getEmployeeNo());
            faceInfo.put("name", face.getName() != null ? face.getName() : "");
            faceInfo.put("gender", face.getGender() != null ? face.getGender() : "male");
            
            // Format for /ISAPI/AccessControl/FaceInfo/SetUp (Face Terminals)
            root.set("FaceInfo", faceInfo);
            json = mapper.writeValueAsString(root);
            endpoint = "/ISAPI/AccessControl/FaceInfo/SetUp?format=json";

            // Decode photo from base64
            byte[] photoBytes = Base64.getDecoder().decode(face.getPhotoBase64());

            log.info("Enrolling face on {} for employee={}", client.getBaseUrl(), face.getEmployeeNo());
            
            String result;
            try {
                result = client.postMultipart(endpoint, json, photoBytes, face.getEmployeeNo() + ".jpg");
            } catch (IsapiException e) {
                if (e.getStatusCode() == 404 || e.getStatusCode() == 405) {
                    log.info("AccessControl endpoint not supported on {}, falling back to FDLib", client.getBaseUrl());
                    // Fallback to /ISAPI/Intelligent/FDLib/FDSetUp/picture (NVRs/Cameras)
                    ObjectNode condRoot = mapper.createObjectNode();
                    ObjectNode cond = mapper.createObjectNode();
                    cond.put("employeeNo", face.getEmployeeNo());
                    cond.put("name", face.getName() != null ? face.getName() : "");
                    condRoot.putArray("FaceInfoCond").add(cond);
                    json = mapper.writeValueAsString(condRoot);
                    endpoint = "/ISAPI/Intelligent/FDLib/FDSetUp/picture";
                    result = client.postMultipart(endpoint, json, photoBytes, face.getEmployeeNo() + ".jpg");
                } else {
                    throw e;
                }
            }

            log.debug("enrollFace result: {}", result);
            return true;
        } catch (Exception e) {
            log.error("enrollFace failed for {}: {}", face.getEmployeeNo(), e.getMessage());
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
