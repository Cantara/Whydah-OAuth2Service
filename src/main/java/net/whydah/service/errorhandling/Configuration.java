package net.whydah.service.errorhandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Configuration {
	private static final Logger log = LoggerFactory.getLogger(Configuration.class);
	private static final Properties properties = new Properties();

	// Tracks which file each property came from
	private static final Map<String, String> propertySources = new HashMap<>();
	// Tracks the override sequence for properties
	private static final Map<String, String> propertyOverrides = new HashMap<>();

	static {
		loadProperties();
		// We don't automatically log properties here anymore, only when explicitly called
	}

	private static void loadProperties() {
		try {
			// Load default properties
			Properties defaultProps = new Properties();
			try (InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream("application.properties")) {
				if (inputStream != null) {
					defaultProps.load(inputStream);
					log.info("Loaded default application.properties with {} properties", defaultProps.size());

					// Add properties and track sources
					for (String key : defaultProps.stringPropertyNames()) {
						properties.setProperty(key, defaultProps.getProperty(key));
						propertySources.put(key, "application.properties");
					}
				} else {
					log.warn("Could not find application.properties in classpath");
				}
			}

			// Try to load overrides from environment-specific properties
			String environment = System.getProperty("env", "local");
			String envPropsFile = "application_" + environment + ".properties";
			Properties envProps = new Properties();
			try (InputStream envInputStream = Configuration.class.getClassLoader().getResourceAsStream(envPropsFile)) {
				if (envInputStream != null) {
					envProps.load(envInputStream);
					log.info("Loaded environment-specific {} with {} properties", envPropsFile, envProps.size());

					// Add properties, track sources and overrides
					for (String key : envProps.stringPropertyNames()) {
						if (properties.containsKey(key)) {
							propertyOverrides.put(key, propertySources.get(key) + " -> " + envPropsFile);
						}
						properties.setProperty(key, envProps.getProperty(key));
						propertySources.put(key, envPropsFile);
					}
				} else {
					log.info("No environment-specific {} found", envPropsFile);
				}
			}

			// Try to load external overrides if specified
			String externalConfigPath = System.getProperty("oauth2.configuration");
			if (externalConfigPath != null) {
				Properties externalProps = new Properties();
				try (FileInputStream externalInputStream = new FileInputStream(externalConfigPath)) {
					externalProps.load(externalInputStream);
					log.info("Loaded external configuration from {} with {} properties",
							externalConfigPath, externalProps.size());

					// Add properties, track sources and overrides
					for (String key : externalProps.stringPropertyNames()) {
						if (properties.containsKey(key)) {
							propertyOverrides.put(key, propertySources.get(key) + " -> " + externalConfigPath);
						}
						properties.setProperty(key, externalProps.getProperty(key));
						propertySources.put(key, externalConfigPath);
					}
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
	 * This should be called explicitly when the service starts.
	 */
	public static void logProperties() {
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
			String key = entry.getKey();
			String source = propertySources.getOrDefault(key, "unknown");
			String override = propertyOverrides.getOrDefault(key, "");

			if (!override.isEmpty()) {
				log.info("{}={} (from: {}, overridden: {})", key, entry.getValue(), source, override);
			} else {
				log.info("{}={} (from: {})", key, entry.getValue(), source);
			}
		}
		log.info("=============================================");

		// Also print to System.out for environments where the logs might not be easily accessible
		System.out.println("\n========== Configuration Properties ==========");
		for (Map.Entry<String, String> entry : sortedProps.entrySet()) {
			String key = entry.getKey();
			String source = propertySources.getOrDefault(key, "unknown");
			String override = propertyOverrides.getOrDefault(key, "");

			if (!override.isEmpty()) {
				System.out.println(key + "=" + entry.getValue() + " (from: " + source + ", overridden: " + override + ")");
			} else {
				System.out.println(key + "=" + entry.getValue() + " (from: " + source + ")");
			}
		}
		System.out.println("=============================================\n");
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

	/**
	 * Returns an unmodifiable map showing which file each property came from.
	 */
	public static Map<String, String> getPropertySources() {
		return Collections.unmodifiableMap(propertySources);
	}

	/**
	 * Returns an unmodifiable map showing the override sequence for properties.
	 */
	public static Map<String, String> getPropertyOverrides() {
		return Collections.unmodifiableMap(propertyOverrides);
	}
}