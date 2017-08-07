package no.cantara.util;

import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;

import java.util.Map;

public class Configuration {
	
	private static final ConstrettoConfiguration configuration = new ConstrettoBuilder()
            .createPropertiesStore()
			.addResource(Resource.create("classpath:application.properties"))
			.addResource(Resource.create("file:./config_override/application_override.properties"))
            .done()
            .getConfiguration();
	
	private Configuration() {}

    public static Map getMap(String key) {
        return configuration.evaluateToMap(String.class, String.class, key);
    }

	public static String getString(String key) {
		return configuration.evaluateToString(key);
	}
	
	public static Integer getInt(String key) {
		return configuration.evaluateToInt(key);
	}

	public static Integer getInt(String key, int defaultValue) {
		return configuration.evaluateTo(key, defaultValue);
	}

	public static boolean getBoolean(String key) {
		return configuration.evaluateToBoolean(key);
	}
}