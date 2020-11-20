package net.whydah.service.oauth2proxyserver;

import java.io.IOException;
import java.security.KeyPair;

import net.whydah.util.RSAKeyHelper;

public class RSAKeyFactory {

	private static KeyPair kp = null;
	private static String keyId = "d9dd37c68d558eee9866cda9bb39ef86"; 
    
	public static String getKid() {
		return keyId;
	}
	
    public static KeyPair getKey()  {
    	if(kp==null) {
    		try {
				kp = RSAKeyHelper.loadKey();
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	if(kp==null) {
    		kp = RSAKeyHelper.makeNewKey();
    		try {
				RSAKeyHelper.saveKey(kp);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	return kp;
    }
    
    public static void deleteKeyFile() {
    	RSAKeyHelper.deleteKeyFile();
    }
}
