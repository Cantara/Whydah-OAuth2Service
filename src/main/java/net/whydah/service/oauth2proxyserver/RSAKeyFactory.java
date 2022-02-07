package net.whydah.service.oauth2proxyserver;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import net.whydah.commands.config.ConstantValues;
import net.whydah.service.authorizations.SSOAuthSession;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;
import net.whydah.util.HazelcastMapHelper;
import net.whydah.util.RSAKeyHelper;

public class RSAKeyFactory {
	
	private static final Logger log = getLogger(RSAKeyFactory.class);

	private static KeyPair kp;
	private static String keyId; 
	private static IMap<String, KeyPair> KEY_PAIRS = HazelcastMapHelper.register("RSAKey_Map");

	static {
		keyId = ConstantValues.RSA_KEY_ID; 
	}
	
	public static String getMyKeyId() {
		return keyId;
	}

	//call from main
	public static void loadKeyConfig() {
		log.info("Load key config");
		if(!KEY_PAIRS.containsKey(keyId)) {
			try {
				//just load the key pair if exists
				kp = RSAKeyHelper.loadKey();
			} catch (Exception e) {
				e.printStackTrace();
			}

			if(kp==null) {
				//compatibility fix
				//do not make a new key pair for the current keyid=d9dd37c68d558eee9866cda9bb39ef86 
				//one oauth2 instance should have its own keyid, and its own key pair
				if((!keyId.equals("d9dd37c68d558eee9866cda9bb39ef86") && keyId.length()>10)
						) {
					kp = RSAKeyHelper.makeNewKey();
					try {
						RSAKeyHelper.saveKey(kp);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			//add to the map
			if(kp!=null) {
				log.info("key pair {} added for keyid {}", kp, keyId);
				KEY_PAIRS.put(keyId, kp);	
			}
		}
	}	

	public static KeyPair getKey() {
		if(!KEY_PAIRS.containsKey(keyId)) {
			loadKeyConfig();
		}
		return kp;
	}

	public static void deleteKeyFile() {
		RSAKeyHelper.deleteKeyFile();
	}

	public static List<JWK> getRsaKeys() {

		return KEY_PAIRS.entrySet().stream().map(entry ->  {

			RSAPublicKey rsa = (RSAPublicKey) entry.getValue().getPublic(); 
			RSAKey key = new RSAKey.Builder(rsa)
					.keyID(entry.getKey())
					.keyUse(new KeyUse("sig"))
					.algorithm(new Algorithm("RS256"))
					.build();
			return key;


		}).collect(Collectors.toList());


	}
}
