package net.whydah.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UriBuilder {
    private String baseUri;
    private Map<String, String> queryParams = new HashMap<>();
    
    private UriBuilder(String baseUri) {
        // Normalize the base URI to handle trailing slashes properly
        this.baseUri = normalizeUri(baseUri);
    }
    
    public static UriBuilder fromUriString(String uri) {
        return new UriBuilder(uri);
    }
    
    public UriBuilder queryParam(String name, Object value) {
        queryParams.put(name, value != null ? value.toString() : "");
        return this;
    }
    
    /**
     * Appends a path segment to the base URI, handling slashes correctly
     */
    public UriBuilder path(String pathSegment) {
        if (pathSegment == null || pathSegment.isEmpty()) {
            return this;
        }
        
        // Remove leading slash from path segment if base URI already ends with slash
        String normalizedPath = pathSegment.startsWith("/") ? pathSegment.substring(1) : pathSegment;
        
        // Ensure base URI ends with slash before appending
        if (!this.baseUri.endsWith("/")) {
            this.baseUri += "/";
        }
        
        this.baseUri += normalizedPath;
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
    
    /**
     * Normalizes URI by handling multiple consecutive slashes and trailing slashes
     */
    private String normalizeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        
        // Handle protocol schemes (http://, https://, etc.)
        String protocol = "";
        String path = uri;
        
        if (uri.contains("://")) {
            int protocolIndex = uri.indexOf("://");
            protocol = uri.substring(0, protocolIndex + 3);
            path = uri.substring(protocolIndex + 3);
        }
        
        // Replace multiple slashes with single slash in the path part
        path = path.replaceAll("/+", "/");
        
        return protocol + path;
    }
    
    /**
     * Static helper method to join URI parts properly
     */
    public static String joinPaths(String basePath, String... pathSegments) {
        StringBuilder result = new StringBuilder(basePath != null ? basePath : "");
        
        for (String segment : pathSegments) {
            if (segment == null || segment.isEmpty()) {
                continue;
            }
            
            // Ensure we have exactly one slash between segments
            if (!result.toString().endsWith("/") && !segment.startsWith("/")) {
                result.append("/");
            } else if (result.toString().endsWith("/") && segment.startsWith("/")) {
                // Remove leading slash from segment to avoid double slash
                segment = segment.substring(1);
            }
            
            result.append(segment);
        }
        
        return result.toString();
    }
}