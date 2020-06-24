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

import org.slf4j.Logger;

public class RSAKeyHelper {
	
	static final Logger log = getLogger(RSAKeyHelper.class);
	static String privateKeyName = "oauth.key";
	static String publicKeyName = "oauth.pub";
	static String currentDir = Paths.get("").toAbsolutePath().toString();
	
	public static KeyPair loadKey() throws Exception {
		
		try {
			
			if(!new File(currentDir + File.separator + privateKeyName).exists() ||
					!new File(currentDir + File.separator + publicKeyName).exists()
					) {
				return null;
			}
			
			log.info("load RSA key from " + currentDir);
			
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
			
		} catch(Exception ex) {
			ex.printStackTrace();
			log.error("Exception: can not load key files - exception: {}", ex);
			throw ex;
		}
	}
	
	public static void saveKey(KeyPair kp) throws IOException {
		try {
			log.info("save RSA key to " + currentDir);
			
			FileOutputStream out = new FileOutputStream(currentDir + File.separator + privateKeyName);
			//out.write("-----BEGIN RSA PRIVATE KEY-----\n");
			out.write(kp.getPrivate().getEncoded());
			//out.write("\n-----END RSA PRIVATE KEY-----\n");
			out.close();

			out = new FileOutputStream(currentDir + File.separator + publicKeyName);
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

	public static void deleteKeyFile() {
		new File(currentDir + File.separator + privateKeyName).delete();
		new File(currentDir + File.separator + publicKeyName).delete();
	}
	
	

}
