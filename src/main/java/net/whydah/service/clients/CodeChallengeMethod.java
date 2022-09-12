package net.whydah.service.clients;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public enum CodeChallengeMethod {
    S256 {
        @Override
        public String transform(String codeVerifier) {
            try {
            	 MessageDigest md = MessageDigest.getInstance("SHA-256");
                 byte[] digest = md.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                 return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    },
    PLAIN {
        @Override
        public String transform(String codeVerifier) {
            return codeVerifier;
        }
    },
    NONE {
        @Override
        public String transform(String codeVerifier) {
            throw new UnsupportedOperationException();
        }
    };

    public abstract String transform(String codeVerifier);
}