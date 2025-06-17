package net.whydah.config;

import net.whydah.service.health.HealthResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // Register packages to scan for JAX-RS resources
        packages("net.whydah");

        // Explicitly register the HealthResource class
        register(HealthResource.class);

        // Register Freemarker for MVC
        register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        property(MvcFeature.TEMPLATE_BASE_PATH, "templates");

        // Enable Spring integration for Jersey
        // This simplified approach should work with Jersey 3.x and Spring 6
        register(org.glassfish.jersey.server.spring.SpringComponentProvider.class);
    }
}