package net.whydah.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.hazelcast.map.IMap;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import net.whydah.commands.config.ConstantValues;

public class RSAKeyFactory {

	private static final Logger log = getLogger(RSAKeyFactory.class);

	private static String keyId = null;

	private static IMap<String, KeyPair> KEY_PAIRS = HazelcastMapHelper.register("RSAKey_Map");

	static final ScheduledExecutorService key_sync_scheduler;


	static {
		keyId = ConstantValues.RSA_KEY_ID;
		if(keyId==null) {
			throw new IllegalArgumentException("oauth2.module.rsa_keyid must be set");
		}
		log.info("Load key config, keyid {}" + keyId);
		RSAKeyHelper.initialzieLocalKeysToMap(keyId, KEY_PAIRS);

		key_sync_scheduler = Executors.newScheduledThreadPool(1);
		key_sync_scheduler.scheduleWithFixedDelay( new Runnable() {

			@Override
			public void run() {
				
				ArrayList<String> keys = new ArrayList<>(KEY_PAIRS.keySet());
				for(String key : keys) {
					try {
						RSAKeyHelper.saveKeyIfNotExist(key, KEY_PAIRS.get(key));
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
				
			}
		}
		,
		5, 10, TimeUnit.SECONDS);
	}

	public static KeyPair getKeyPair() {
		return KEY_PAIRS.get(keyId);
	}

	public static String getKId() {
		return keyId;
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

	public static Key findPublicKey(String keyId) {
		return KEY_PAIRS.get(keyId).getPublic();
	}
}
