package net.whydah.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Configuration {
	private static final Logger log = LoggerFactory.getLogger(Configuration.class);
	private static final Properties properties = new Properties();

	static {
		loadProperties();
		logProperties();
	}

	private static void loadProperties() {
		try {
			// Load default properties
			try (InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream("application.properties")) {
				if (inputStream != null) {
					properties.load(inputStream);
					log.info("Loaded default application.properties");
				} else {
					log.warn("Could not find application.properties in classpath");
				}
			}

			// Try to load overrides from environment-specific properties
			String environment = System.getProperty("env", "local");
			try (InputStream envInputStream = Configuration.class.getClassLoader().getResourceAsStream("application_" + environment + ".properties")) {
				if (envInputStream != null) {
					properties.load(envInputStream);
					log.info("Loaded environment-specific application_{}.properties", environment);
				} else {
					log.info("No environment-specific application_{}.properties found", environment);
				}
			}

			// Try to load external overrides if specified
			String externalConfigPath = System.getProperty("oauth2.configuration");
			if (externalConfigPath != null) {
				try (FileInputStream externalInputStream = new FileInputStream(externalConfigPath)) {
					properties.load(externalInputStream);
					log.info("Loaded external configuration from {}", externalConfigPath);
				}
			} else {
				log.info("No external configuration path specified (oauth2.configuration)");
			}
		} catch (IOException e) {
			log.error("Failed to load configuration properties", e);
		}
	}

	/**
	 * Logs all loaded properties in alphabetical order.
	 * Sensitive properties (containing 'password', 'secret', 'key') are masked.
	 */
	private static void logProperties() {
		// Create a sorted view of properties for cleaner logging
		Map<String, String> sortedProps = new TreeMap<>();
		for (String key : properties.stringPropertyNames()) {
			String value = properties.getProperty(key);
			// Mask sensitive values
			if (key.toLowerCase().contains("password") ||
					key.toLowerCase().contains("secret") ||
					key.toLowerCase().contains("key")) {
				value = "********";
			}
			sortedProps.put(key, value);
		}

		log.info("========== Configuration Properties ==========");
		for (Map.Entry<String, String> entry : sortedProps.entrySet()) {
			log.info("{}={}", entry.getKey(), entry.getValue());
		}
		log.info("=============================================");

		// Also print to System.out for environments where the logs might not be easily accessible
		System.out.println("========== Configuration Properties ==========");
		for (Map.Entry<String, String> entry : sortedProps.entrySet()) {
			System.out.println(entry.getKey() + "=" + entry.getValue());
		}
		System.out.println("=============================================");
	}

	public static String getString(String key) {
		return properties.getProperty(key);
	}

	public static String getString(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public static int getInt(String key) {
		String value = getString(key);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				log.warn("Failed to parse integer value for key: {}", key, e);
			}
		}
		return 0;
	}

	public static int getInt(String key, int defaultValue) {
		String value = getString(key);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				log.warn("Failed to parse integer value for key: {}", key, e);
			}
		}
		return defaultValue;
	}

	public static boolean getBoolean(String key) {
		String value = getString(key);
		return Boolean.parseBoolean(value);
	}

	public static boolean getBoolean(String key, boolean defaultValue) {
		String value = getString(key);
		if (value != null) {
			return Boolean.parseBoolean(value);
		}
		return defaultValue;
	}

	/**
	 * Returns an unmodifiable view of all properties.
	 * This is useful for debugging or displaying configuration.
	 */
	public static Map<String, String> getAllProperties() {
		Map<String, String> result = new HashMap<>();
		for (String key : properties.stringPropertyNames()) {
			result.put(key, properties.getProperty(key));
		}
		return Collections.unmodifiableMap(result);
	}
}