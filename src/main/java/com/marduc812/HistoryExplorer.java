package com.marduc812;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.*;
import burp.api.montoya.scope.Scope;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryExplorer {
    static Logging logging;
    HistoryExplorerGui gui;
    static Scope scope;
    private ExecutorService executorService;

    public HistoryExplorer(MontoyaApi api, HistoryExplorerGui gui, String searchTerm, boolean regExSearch, boolean inScopeSearch ,boolean[] searchStatusCodes, String includedExtensionsString, String excludedExtensionsString, List<Boolean> httpOptions) {

        logging = api.logging();
        scope = api.scope();
        this.gui = gui;

        String[] excludedExtensions;
        String[] includedExtensions;
        List<String> statusFilters = getStatusFilters(searchStatusCodes);

        if (searchTerm == null || searchTerm.isEmpty()) {
            logging.logToOutput("Empty search string");
            return;
        }

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

        // all request filters are disabled
        if (statusFilters.isEmpty()) {
            return;
        }

        if (!httpOptions.get(0) && !httpOptions.get(1)) {
            // Both reqSearch and resSearch are false
            logging.logToOutput("At least one of the HTTP options should be enabled");
            return;
        }

        this.executorService = Executors.newFixedThreadPool(4);
        executorService.submit(() -> {
            processHttpHistory(api, gui, searchTerm, regExSearch, inScopeSearch, searchStatusCodes, includedExtensions, excludedExtensions, httpOptions);
        });
    }

    private void processHttpHistory(MontoyaApi api, HistoryExplorerGui gui, String searchTerm, boolean regExSearch, boolean inScopeSearch, boolean[] searchStatusCodes, String[] includedExtensions, String[] excludedExtensions, List<Boolean> httpOptions) {

        List<String> statusFilters = getStatusFilters(searchStatusCodes);
        FilterHTTPResults filterHTTP = new FilterHTTPResults(searchTerm);
        List<ProxyHttpRequestResponse> httpHistory = api.proxy().history(filterHTTP);
        Map<String, Set<String>> hostToServersMap = new HashMap<>();
        Pattern pattern = Pattern.compile(searchTerm);
        Boolean reqFilter = httpOptions.get(0);
        Boolean resFilter = httpOptions.get(1);

        logging.logToOutput("#############\nRecords returned: " + httpHistory.size() + "\n##########\n");

        httpHistory.forEach(item -> {
            /// Check for null values
        if (item.originalResponse() != null && item.finalRequest() != null) {

            // If the status code is not selected ignore it
            if (!statusFilters.contains(String.valueOf(item.originalResponse().statusCode()).substring(0, 1))) {
                return;
            }

            // If inScopeSearch is enabled, ignore items which are not in scope
            if (inScopeSearch) {
                if (!scope.isInScope(item.url())) {
                    return;
                }
            }

            // Get the file extension
            String requestExtensionStr = String.valueOf(getExtensionFromPath(item.path()));

            // Ignore if the request URL is in the excluded type of extension
            if (excludedExtensions.length > 0 && requestExtensionStr != null && Arrays.asList(excludedExtensions).contains(requestExtensionStr)) {
                return;
            }

            // Ignore if the request URL is in not the included type of extension
            if (includedExtensions.length > 0 && requestExtensionStr != null && !Arrays.asList(includedExtensions).contains(requestExtensionStr)) {
                return;
            }

            List<String> matchingValues = new ArrayList<>();

            // Search using RegEx or Literal String
            if (regExSearch) {
                // Check in the request
                if (reqFilter) {
                    Matcher requestMatcher = pattern.matcher(item.finalRequest().toString());
                    while (requestMatcher.find()) {
                        matchingValues.add(requestMatcher.group());
                    }
                }

                // Check in the response
                if (resFilter) {
                    Matcher responseMatcher = pattern.matcher(item.originalResponse().toString());
                    while (responseMatcher.find()) {
                        matchingValues.add(responseMatcher.group());
                    }
                }
            } else {
                // not a regex search
                if (reqFilter){
                    String request = item.finalRequest().toString();
                    if (request.contains(searchTerm)) {
                        matchingValues.add(searchTerm);
                    }
                }
                if (resFilter) {
                    String response = item.originalResponse().toString();
                    if (response.contains(searchTerm)) {
                        matchingValues.add(searchTerm);
                    }
                }
            }

            if (!matchingValues.isEmpty()) {
                Set<String> uniqueMatchingValues = new HashSet<>(matchingValues);

                String host = item.host();
                hostToServersMap.putIfAbsent(host, new HashSet<>());

                Set<String> matchesForHost = hostToServersMap.get(host);
                matchesForHost.addAll(uniqueMatchingValues);
            }
        }
    });

        httpHistory.clear();
        httpHistory = null;
        System.gc();

        Map<String, String> newData = new LinkedHashMap<>();

        hostToServersMap.forEach((host, parsedValues) -> {
            String parsedString = String.join(" || ", parsedValues);
            newData.put(host, parsedString);
        });

        // update the GUI
        java.awt.EventQueue.invokeLater(() -> {
            gui.enableSearchButton();
            gui.updateTableData(newData);
        });
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
            logging.logToError("Error Parsing Extension: " + e);
        }
        return "none";
    }
}
