package com.marduc812;
import burp.api.montoya.proxy.ProxyHistoryFilter;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import java.util.regex.Pattern;

public class FilterHTTPResults implements ProxyHistoryFilter {
    private final Pattern pattern;

    public FilterHTTPResults(String searchTerm) {
        this.pattern = Pattern.compile(searchTerm);
    }

    @Override
    public boolean matches(ProxyHttpRequestResponse requestResponse) {
        // You can check request or response or both. Example for checking response body:

        String request = String.valueOf(requestResponse.finalRequest());
        String response = "";
        if (requestResponse.originalResponse() != null) {
            response = String.valueOf(requestResponse.originalResponse());
        }

        boolean resMatch = pattern.matcher(response).find();
        boolean reqMatch = pattern.matcher(request).find();

        return  resMatch || reqMatch;
    }

}
