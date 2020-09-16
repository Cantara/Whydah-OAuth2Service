package net.whydah.service.oauth2proxyserver;

import java.security.KeyPair;

import net.whydah.util.RSAKeyHelper;

public class RSAKeyFactory {

	private static KeyPair kp = null;
	private static String keyId = "d9dd37c68d558eee9866cda9bb39ef86"; 
    
	public static String getKid() {
		return keyId;
	}
	
    public static KeyPair getKey() throws Exception {
    	if(kp==null) {
    		kp = RSAKeyHelper.loadKey();
    	}
    	if(kp==null) {
    		kp = RSAKeyHelper.makeNewKey();
    		RSAKeyHelper.saveKey(kp);
    	}
    	return kp;
    }
    
    public static void deleteKeyFile() {
    	RSAKeyHelper.deleteKeyFile();
    }
}
