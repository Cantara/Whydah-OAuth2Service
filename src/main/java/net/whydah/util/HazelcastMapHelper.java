package net.whydah.util;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.MapListener;

public class HazelcastMapHelper {

	private final static Logger log = LoggerFactory.getLogger(HazelcastMapHelper.class);
	static Config hazelcastConfig;
	static String tag ="";

	static {
		String xmlFileName = System.getProperty("hazelcast.config");
		log.info("Loading hazelcast configuration from :" + xmlFileName);
		hazelcastConfig = new Config();
		if (xmlFileName != null && xmlFileName.length() > 10) {
			try {
				hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
				log.info("Loading hazelcast configuration from :" + xmlFileName);
			} catch (FileNotFoundException notFound) {
				log.error("Error - not able to load hazelcast.xml configuration.  Using embedded as fallback");
			}
		}
		hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");

		try {
			AwsConfig awsConfig = hazelcastConfig.getNetworkConfig().getJoin().getAwsConfig();
			tag = awsConfig.getProperty("tag-value")!=null?awsConfig.getProperty("tag-value"):"";
			
		} catch(Exception ex) {

		}
	}

	public static IMap register(String name, MapListener listener) {

		HazelcastInstance hazelcastInstance;
		try {
			hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
		} catch(Exception ex) {
			hazelcastInstance = Hazelcast.newHazelcastInstance();
		}

		log.info("Connectiong to map {}", tag + "_" + name);
		
		
		
		IMap result = hazelcastInstance.getMap(tag + "_" + name);
		if(listener!=null) {
			result.addEntryListener(listener, true);
		}
		return result;

	}
	
	public static IMap register(String name) {
		return register(name, null);
	}
}
