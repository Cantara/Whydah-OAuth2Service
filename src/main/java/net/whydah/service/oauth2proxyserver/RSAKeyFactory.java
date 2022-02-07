package net.whydah.service.oauth2proxyserver;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Iterator;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import net.whydah.service.authorizations.SSOAuthSession;
import net.whydah.util.HazelcastMapHelper;
import net.whydah.util.RSAKeyHelper;

public class RSAKeyFactory {

	private static KeyPair kp = null;
	private static String keyId = "d9dd37c68d558eee9866cda9bb39ef86"; 

	private static IMap<String, KeyPair> keys = HazelcastMapHelper.register("RSAKey_Map");

	public static String getKid() {
		return keyId;
	}

	public static boolean isLeader() {
		Iterator<HazelcastInstance> iter = Hazelcast.getAllHazelcastInstances().iterator();
		if (iter.hasNext()) { // cluster mode 
			HazelcastInstance instance = iter.next();
			return instance.getCluster().getMembers().iterator().next().localMember();
		} else {
			return true; // standalone mode
		}
	}

	public static KeyPair getKey()  {
		//ask the leader to create a key
		if(!keys.containsKey(keyId)|| keys.get(keyId)==null) {
			if(isLeader()) {
				KeyPair kp = null;
				try {
					kp = RSAKeyHelper.loadKey();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(kp==null) {
					kp = RSAKeyHelper.makeNewKey();
					try {
						RSAKeyHelper.saveKey(kp);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				keys.put(keyId, kp);
			} else {
				deleteKeyFile();
			}
		}
		return keys.get(keyId);
	}

	public static void deleteKeyFile() {
		RSAKeyHelper.deleteKeyFile();
	}
}
