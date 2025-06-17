package net.whydah.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // CRITICAL: Register Spring integration components FIRST
        register(SpringLifecycleListener.class);
        register(RequestContextFilter.class);

        // Let Spring manage all resources via package scanning
        //packages("net.whydah.service.oauth2proxyserver");
        //packages("net.whydah.service.authorizations");
        //packages("net.whydah.service.health");
        packages("net.whydah.service");

        // Register filters and other providers
        register(net.whydah.service.CorsFilter.class);

        // Register Freemarker for MVC
        register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        property(MvcFeature.TEMPLATE_BASE_PATH, "templates");

        // Enable Spring integration - Jersey will use Spring-managed instances
        property("jersey.config.server.spring.context.inject", true);

        // Let Jersey discover Spring-managed resources
        property("jersey.config.server.provider.scanning.recursive", true);
    }
}