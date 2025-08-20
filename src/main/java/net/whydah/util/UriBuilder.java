// Create a new file: src/main/java/net/whydah/util/UriBuilder.java
package net.whydah.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UriBuilder {
    private String baseUri;
    private Map<String, String> queryParams = new HashMap<>();
    
    private UriBuilder(String baseUri) {
        this.baseUri = baseUri;
    }
    
    public static UriBuilder fromUriString(String uri) {
        return new UriBuilder(uri);
    }
    
    public UriBuilder queryParam(String name, Object value) {
        queryParams.put(name, value != null ? value.toString() : "");
        return this;
    }
    
    public String build() {
        if (queryParams.isEmpty()) {
            return baseUri;
        }
        
        StringBuilder uri = new StringBuilder(baseUri);
        uri.append("?");
        
        boolean first = true;
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!first) {
                uri.append("&");
            }
            try {
                uri.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                   .append("=")
                   .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                uri.append(entry.getKey()).append("=").append(entry.getValue());
            }
            first = false;
        }
        
        return uri.toString();
    }
    
    public String toUriString() {
        return build();
    }
}