package net.whydah.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.hazelcast.map.IMap;

import net.whydah.commands.config.ConstantValues;

public class RSAKeyHelper {

	static final Logger log = getLogger(RSAKeyHelper.class);
	static String currentDir = Paths.get("").toAbsolutePath().toString();

	public static void initialzieLocalKeysToMap(String keyId, IMap<String, KeyPair>  map)  {

		try {
			//find [keyid].key and [keyid].pub keypair
			List<String> privatekeyfiles =  FileUtil.findFiles(Paths.get(currentDir), "key").stream().map(i -> FileUtil.getBaseName(Paths.get(i).getFileName().toString())).collect(Collectors.toList());
			List<String> publickeyfiles =  FileUtil.findFiles(Paths.get(currentDir), "pub").stream().map(i -> FileUtil.getBaseName(Paths.get(i).getFileName().toString())).collect(Collectors.toList());
			publickeyfiles.retainAll(privatekeyfiles);

			for(String i : publickeyfiles) 
			{
				try {
					//compatibility fix for old version
					if(i.equalsIgnoreCase("oauth")) {
						if(!map.containsKey("d9dd37c68d558eee9866cda9bb39ef86")) {
							KeyPair k = getKey("oauth.key", "oauth.pub");
							map.put("d9dd37c68d558eee9866cda9bb39ef86", getKey("oauth.key", "oauth.pub"));
							saveKey("d9dd37c68d558eee9866cda9bb39ef86", k);
							try {
								//delete old format
								new File(currentDir + File.separator + "oauth.key").delete();
								new File(currentDir + File.separator + "oauth.pub").delete();
							}	catch(Exception ex) {
								ex.printStackTrace();
								log.error("Cannot delete file {} {} - exception {}", currentDir + File.separator + "oauth.key", currentDir + File.separator + "oauth.pub", ex.getMessage());
							}
						} else {
							try {
								//delete old format
								new File(currentDir + File.separator + "oauth.key").delete();
								new File(currentDir + File.separator + "oauth.pub").delete();
							} catch(Exception ex) {
								ex.printStackTrace();
								log.error("Cannot delete file {} {}", currentDir + File.separator + "oauth.key", currentDir + File.separator + "oauth.pub");
							}
						}
					} else {
						if(!map.containsKey(i)) {
							map.put(i, getKey(i + ".key", i + ".pub"));	
						}
					}
				}	catch(Exception ex) {
					ex.printStackTrace();
					log.error("Cannot load key id {} {}", i, ex.getMessage());
				}
			};

			if(!map.containsKey(keyId)) {
				KeyPair k =  makeNewKey();
				saveKey(keyId, k);
				map.put(keyId, k);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			log.error("Cannot load loadKeysToMap - exception {}", ex.getMessage());
		}


	}

	public static KeyPair getKey(String privateKeyName, String publicKeyName) throws Exception {

		/* Read all bytes from the private key file */
		Path path = Paths.get(currentDir + File.separator + privateKeyName);
		byte[] bytes = Files.readAllBytes(path);

		KeyFactory kf = KeyFactory.getInstance("RSA");
		/* Generate private key. */
		PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
		PrivateKey pvt = kf.generatePrivate(ks);

		/* Read all bytes from the public key file */
		path = Paths.get(currentDir + File.separator + publicKeyName);
		bytes = Files.readAllBytes(path);

		/* Generate public key. */
		X509EncodedKeySpec ks2 = new X509EncodedKeySpec(bytes);
		PublicKey pub = kf.generatePublic(ks2);

		return new KeyPair(pub, pvt);

	}

	public static void saveKey(String keyId, KeyPair kp) throws IOException {
		try {
			log.info("save RSA key to " + currentDir);

			FileOutputStream out = new FileOutputStream(currentDir + File.separator + keyId + ".key");
			//out.write("-----BEGIN RSA PRIVATE KEY-----\n");
			out.write(kp.getPrivate().getEncoded());
			//out.write("\n-----END RSA PRIVATE KEY-----\n");
			out.close();

			out = new FileOutputStream(currentDir + File.separator + keyId + ".pub");
			//out.write("-----BEGIN RSA PUBLIC KEY-----\n");
			out.write(kp.getPublic().getEncoded());
			//out.write("\n-----END RSA PUBLIC KEY-----\n");
			out.close();
		} catch(IOException ex) {
			log.error("IOEXception: can not save key files - exception: {}", ex);
			throw ex;
		}
	}

	public static KeyPair makeNewKey() {
		KeyPairGenerator kpg;
		try {
			kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);

			KeyPair kp = kpg.generateKeyPair();
			return kp;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void deleteKeyFile(String keyId) {
		new File(currentDir + File.separator + keyId + ".key").delete();
		new File(currentDir + File.separator + keyId + ".pub").delete();
	}

	
	public static void saveKeyIfNotExist(String keyId, KeyPair key) throws IOException {
		if(!new File(currentDir + File.separator + keyId + ".pub").exists() || 
				!new File(currentDir + File.separator + keyId + ".key").exists()) {
			saveKey(keyId, key);
		}
	}



}
