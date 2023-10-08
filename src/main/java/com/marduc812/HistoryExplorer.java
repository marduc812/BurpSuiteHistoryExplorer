package com.marduc812;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.*;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryExplorer {

    static Logging logging;
    HistoryExplorerGui gui;

    public HistoryExplorer(MontoyaApi api, HistoryExplorerGui gui, String searchTerm, boolean regExSearch ,boolean[] searchStatusCodes, String includedExtensionsString, String excludedExtensionsString) {

        logging = api.logging();
        this.gui = gui;

        if (searchTerm == null || searchTerm.isEmpty()) {
            logging.logToOutput("Empty search string");
            return;
        }

        String[] excludedExtensions;
        String[] includedExtensions;

        if (excludedExtensionsString.isEmpty()) {
            excludedExtensions = new String[0];
        } else {
            excludedExtensions = excludedExtensionsString.split(",");
            for (int i = 0; i < excludedExtensions.length; i++) {
                excludedExtensions[i] = excludedExtensions[i].trim();
            }
        }

        if (includedExtensionsString.isEmpty()) {
            includedExtensions = new String[0];
        } else {
            includedExtensions = includedExtensionsString.split(",");
            for (int i = 0; i < includedExtensions.length; i++) {
                includedExtensions[i] = includedExtensions[i].trim();
            }
        }

        List<String> statusFilters = getStatusFilters(searchStatusCodes);

        // all request filters are disabled
        if (statusFilters.isEmpty()) {
            return;
        }

        List<ProxyHttpRequestResponse> httpHistory = api.proxy().history();
        Map<String, Set<String>> hostToServersMap = new HashMap<>();
        Pattern pattern = Pattern.compile(searchTerm, Pattern.MULTILINE);

        httpHistory.forEach(item ->
        {
//          Check for null values
            if (item.originalResponse() != null && item.finalRequest() != null) {

                // If the status code is not selected ignore it
                if (!statusFilters.contains(String.valueOf(item.originalResponse().statusCode()).substring(0, 1))) {
                    return;
                }

                String requestExtensionStr = String.valueOf(getExtensionFromPath(item.path()));


                if (excludedExtensions.length > 0 && requestExtensionStr != null && Arrays.asList(excludedExtensions).contains(requestExtensionStr)) {
                    return;
                }


                if (includedExtensions.length > 0 && requestExtensionStr != null && !Arrays.asList(includedExtensions).contains(requestExtensionStr)) {
                    return;
                }

                String matchingValue = null;

                if (regExSearch) {
                    // regex search
                    Matcher matcherRes = pattern.matcher(item.originalResponse().toString());
                    if (matcherRes.find()) {
                        matchingValue = matcherRes.group();
                    }
                } else {
                    // exact string search
                    String response = item.originalResponse().toString();
                    if (response.contains(searchTerm)) {
                        matchingValue = searchTerm;
                    }
                }

                if (matchingValue != null) {
                    String host = item.host();

                    // Initialize the list for this host if it doesn't exist yet
                    hostToServersMap.putIfAbsent(host, new HashSet<>());

                    // Add the matching value to the list for this host
                    hostToServersMap.get(host).add(matchingValue);
                }
            }
        });


        Map<String, String> newData = new LinkedHashMap<>();

        hostToServersMap.forEach((host, serverValues) -> {
            String servers = String.join(" || ", serverValues);
            newData.put(host, servers);
        });

        gui.updateTableData(newData);
    }

    private List<String> getStatusFilters(boolean[] statusFilter) {

        List<String> statusList = new ArrayList<>();
        if (statusFilter[0]) {
            statusList.add("2");
        }
        if (statusFilter[1]) {
            statusList.add("3");
        }
        if (statusFilter[2]) {
            statusList.add("4");
        }
        if (statusFilter[3]) {
            statusList.add("5");
        }

        return statusList;
    }

    public static String getExtensionFromPath(String urlPathString) {
        try {
            // Remove query parameters if present
            int queryParamPos = urlPathString.indexOf('?');
            if (queryParamPos != -1) {
                urlPathString = urlPathString.substring(0, queryParamPos);
            }

            int lastDotPos = urlPathString.lastIndexOf('.');
            int lastSlashPos = urlPathString.lastIndexOf('/');

            // Check if the last dot comes after the last slash and is not the last character in the string
            if (lastDotPos > lastSlashPos && lastDotPos < urlPathString.length() - 1) {
                return urlPathString.substring(lastDotPos + 1);
            }
        } catch (Exception e) {
            logging.logToError(String.valueOf("Error Parsing Extension: " + e));
        }
        return "none";
    }



}
