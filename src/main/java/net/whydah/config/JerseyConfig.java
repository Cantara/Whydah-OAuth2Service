package net.whydah.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // CRITICAL: Enable Spring integration FIRST before anything else
        register(org.glassfish.jersey.server.spring.SpringComponentProvider.class);

        // Configure Jersey to use Spring for ALL dependency injection
        property("jersey.config.server.provider.classnames",
                "org.glassfish.jersey.server.spring.SpringComponentProvider");

        // Enable package scanning to find Spring-managed resources
        packages("net.whydah.service");

        // Register Freemarker for MVC
        register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        property(MvcFeature.TEMPLATE_BASE_PATH, "templates");

        // Disable Jersey's own resource management in favor of Spring
        property("jersey.config.server.provider.scanning.recursive", true);

        // Force Jersey to prefer Spring-managed instances
        property("jersey.config.server.resource.validation.disable", true);
    }
}