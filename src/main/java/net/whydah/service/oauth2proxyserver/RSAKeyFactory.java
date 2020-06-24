package net.whydah.service.oauth2proxyserver;

import java.security.KeyPair;

import net.whydah.util.RSAKeyHelper;

public class RSAKeyFactory {

	private static KeyPair kp = null;
    
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
