package net.whydah.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.spec.KeySpec;
import java.util.Base64;

public class ClientIDUtil {

    private static String padding;
    private static String keyPassword;
    private static Key key;
    private static final Logger log = LoggerFactory.getLogger(ClientIDUtil.class);
    private static SecretKeyFactory factory;// = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    static IvParameterSpec iv;

    static {
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            iv = new IvParameterSpec("0000000000000000".getBytes());
            padding = Configuration.getString("oauth2.module.padding");
            keyPassword = Configuration.getString("oauth2.module.keysecret");
            log.info("Resolved oauth padding and keysecret from configuration");
            key = generateNewKey(keyPassword);
        } catch (Exception e) {
            padding = "11111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
            keyPassword = "myKeyPassword";
            key = generateNewKey(keyPassword);
            log.error("Error in resolving oauth padding and keysecret from configuration - using built-in fallback");
        }
        log.info("padding:" + padding);
        log.info("keyPassword:" + keyPassword);
    }

    public static String getApplicationId(String clientId) {
        String applicationId = null;
       
        if (clientId != null && !clientId.isEmpty()) {
            key = generateNewKey(keyPassword);
            applicationId = xorHex(decrypt(clientId, key), padding);
        }
        return applicationId;
    }

    public static String getClientID(String applicationId) {
        log.info("Resolving applicationId:" + applicationId);
        if (applicationId == null || applicationId.length() < 3) {
            return "null";
        }
        String xorString = xorHex(applicationId, padding);
        log.info("Padded applicationId:" + xorString);

        //if (key == null) {
            key = generateNewKey(keyPassword);
        //}

        String clientID = encrypt(xorString, key);
        log.info("Resolved clientId:" + clientID);
        return clientID;
    }

    public static String xorHex(String a, String b) {
        // TODO: Validation
        char[] chars = new char[a.length()];
        for (int i = 0; i < chars.length; i++) {
            if (a.charAt(i) == '-') {
                chars[i] = a.charAt(i);
            } else {
                chars[i] = toHex(fromHex(a.charAt(i)) ^ fromHex(b.charAt(i)));
            }
        }
        return new String(chars).toLowerCase();  // We use lower-case UUID as applicationIDs
    }

    private static int fromHex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        throw new IllegalArgumentException();
    }

    private static char toHex(int nybble) {
        if (nybble < 0 || nybble > 15) {
            throw new IllegalArgumentException();
        }
        return "0123456789ABCDEF".charAt(nybble);
    }

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    public static String encrypt(final String valueEnc) {
        //if (key == null) {
        key = generateNewKey(keyPassword);
        //}
        return encrypt(valueEnc, key);
    }
    
    public static String encrypt(final String valueEnc, final Key key) {

        String encryptedVal = null;

        try {
            final Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, key, iv);
            final byte[] encValue = c.doFinal(valueEnc.getBytes());
            encryptedVal = Base64.getEncoder().encodeToString(encValue);
        } catch (Exception ex) {
            log.warn("key:" + key);
            log.warn("encryptedValue:" + valueEnc);
            log.error("The Exception is=", ex);
        }

        return validateEncodedString(encryptedVal);
    }
    
    public static String decrypt(final String encryptedValue) {
        //if (key == null) {
        key = generateNewKey(keyPassword);
        //}
        return decrypt(encryptedValue, key);
    }

    private static String decrypt(final String encryptedValue, final Key key) {

        String decryptedValue = null;

        try {

            final Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key, iv);
            final byte[] decorVal = Base64.getDecoder().decode(validateDecodedString(encryptedValue));
            final byte[] decValue = c.doFinal(decorVal);
            decryptedValue = new String(decValue);
        } catch (Exception ex) {
            log.warn("key:" + key);
            log.warn("encryptedValue:" + encryptedValue);
            log.error("The Exception is=", ex);
        }

        return decryptedValue;
    }

    private static Key generateNewKey(String keypassword) {
        log.info("generateNewKey-keyPassword:" + keypassword);
        log.info("generateNewKey-padding:" + padding);
        try {
//            String paddedKeyPass=xorHex(keypassword.toCharArray(),padding);
            String paddedKeyPass = keypassword + padding.substring(keypassword.length());
            paddedKeyPass = paddedKeyPass.substring(0, 15);
            log.info("generateNewKey-paddedKeyPass:" + paddedKeyPass);
            char[] password = paddedKeyPass.toCharArray();
            byte[] salt = "jkjk".getBytes();
            /* Derive the key, given password and salt. */
            KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
            return secret;
        } catch (Exception e) {
            log.info("generateNewKey-padding:" + padding);
            log.error("The Exception is=", e);
            return null;
        }
    }
    
    public static String validateEncodedString(String base64Input)
    {
        return base64Input.replace('+', '.').replace('/', '_').replace('=', '-');
    }

    public static String validateDecodedString(String encodedBase64Input)
    {
        return encodedBase64Input.replace('.', '+').replace('_', '/').replace('-', '=');
    }


}
