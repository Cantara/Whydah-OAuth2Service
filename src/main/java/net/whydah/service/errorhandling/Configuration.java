package net.whydah.service.errorhandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
	private static final Logger log = LoggerFactory.getLogger(Configuration.class);
	private static final Properties properties = new Properties();

	static {
		loadProperties();
	}

	private static void loadProperties() {
		try {
			// Load default properties
			try (InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream("application.properties")) {
				if (inputStream != null) {
					properties.load(inputStream);
				}
			}

			// Try to load overrides from environment-specific properties
			String environment = System.getProperty("env", "local");
			try (InputStream envInputStream = Configuration.class.getClassLoader().getResourceAsStream("application_" + environment + ".properties")) {
				if (envInputStream != null) {
					properties.load(envInputStream);
				}
			}

			// Try to load external overrides if specified
			String externalConfigPath = System.getProperty("oauth2.configuration");
			if (externalConfigPath != null) {
				try (FileInputStream externalInputStream = new FileInputStream(externalConfigPath)) {
					properties.load(externalInputStream);
				}
			}
		} catch (IOException e) {
			log.error("Failed to load configuration properties", e);
		}
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
}