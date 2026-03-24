package com.isup.isapi.modules;

import com.isup.isapi.IsapiClient;
import com.isup.isapi.IsapiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User/employee management on access control devices.
 * Handles card, face, and fingerprint enrollment.
 */
public class UserModule {
    private static final Logger log = LoggerFactory.getLogger(UserModule.class);
    private final IsapiClient client;

    public UserModule(IsapiClient client) {
        this.client = client;
    }

    /** POST /ISAPI/AccessControl/UserInfo/SetUp - add/update user */
    public String addUser(String employeeNo, String name, String cardNo, String userType) {
        String body = String.format(
            "{\"UserInfo\":{\"employeeNo\":\"%s\",\"name\":\"%s\",\"userType\":\"%s\"," +
            "\"Valid\":{\"enable\":true,\"beginTime\":\"2000-01-01T00:00:00\",\"endTime\":\"2030-12-31T23:59:59\"}," +
            "\"belongGroup\":\"1\",\"password\":\"\",\"doorRight\":\"1\"," +
            "\"RightPlan\":[{\"doorNo\":1,\"planTemplateNo\":\"1\"}]," +
            "\"cardList\":[{\"cardNo\":\"%s\",\"cardType\":\"normalCard\"}]}}",
            employeeNo, name, userType, cardNo != null ? cardNo : "");
        try {
            return client.post("/ISAPI/AccessControl/UserInfo/SetUp", body);
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** DELETE /ISAPI/AccessControl/UserInfo/Delete - delete user */
    public String deleteUser(String employeeNo) {
        String body = String.format(
            "{\"UserInfoDelCond\":{\"EmployeeNoList\":[{\"employeeNo\":\"%s\"}]}}", employeeNo);
        try {
            return client.post("/ISAPI/AccessControl/UserInfo/Delete", body);
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** POST /ISAPI/AccessControl/UserInfo/Search - search users on device */
    public String searchUsers(int maxResults, int startPos) {
        String body = String.format(
            "{\"UserInfoSearchCond\":{\"searchID\":\"1\",\"maxResults\":%d,\"searchResultPostion\":%d," +
            "\"EmployeeNoList\":[{\"employeeNo\":\"\"}]}}",
            maxResults, startPos);
        try {
            return client.post("/ISAPI/AccessControl/UserInfo/Search", body);
        } catch (IsapiException e) {
            log.debug("searchUsers: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("searchUsers: {}", e.getMessage());
            return "{}";
        }
    }

    /** GET /ISAPI/AccessControl/CardInfo/Search - search cards */
    public String searchCards(String employeeNo) {
        String body = String.format(
            "{\"CardInfoSearchCond\":{\"searchID\":\"1\",\"maxResults\":20,\"searchResultPostion\":0," +
            "\"EmployeeNoList\":[{\"employeeNo\":\"%s\"}]}}", employeeNo);
        try {
            return client.post("/ISAPI/AccessControl/CardInfo/Search", body);
        } catch (IsapiException e) {
            log.debug("searchCards: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("searchCards: {}", e.getMessage());
            return "{}";
        }
    }

    /** POST /ISAPI/AccessControl/AcsEvent?format=json - query access events */
    public String getAccessEvents(String startTime, String endTime, int maxResults) {
        String body = String.format(
            "{\"AcsEventCond\":{\"searchID\":\"1\",\"searchResultPostion\":0,\"maxResults\":%d," +
            "\"startTime\":\"%s\",\"endTime\":\"%s\",\"major\":0,\"minor\":0}}",
            maxResults, startTime, endTime);
        try {
            return client.post("/ISAPI/AccessControl/AcsEvent?format=json", body);
        } catch (IsapiException e) {
            log.debug("getAccessEvents: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getAccessEvents: {}", e.getMessage());
            return "{}";
        }
    }
}
