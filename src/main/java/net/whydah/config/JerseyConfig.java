package net.whydah.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // DO NOT register packages - let Spring manage the resources
        // packages("net.whydah");

        // DO NOT explicitly register resources - Spring will provide them
        // register(HealthResource.class);

        // Register Freemarker for MVC
        register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        property(MvcFeature.TEMPLATE_BASE_PATH, "templates");

        // IMPORTANT: Enable Jersey-Spring integration
        // This tells Jersey to use Spring's ApplicationContext for dependency injection
        register(org.glassfish.jersey.server.spring.SpringComponentProvider.class);

        // Make sure Jersey doesn't try to manage Spring-annotated classes itself
        property("jersey.config.server.provider.scanning.recursive", false);

        // Let Spring handle the resource discovery and management
        property("jersey.config.server.provider.packages", "");
    }
}




