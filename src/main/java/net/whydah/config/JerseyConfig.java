package net.whydah.config;

import org.glassfish.hk2.utilities.ImmediateScopeModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
    	// Let Spring manage all resources via package scanning
        packages("net.whydah.service");
        
        // CRITICAL: Register Spring integration components FIRST
        register(SpringLifecycleListener.class);
        register(RequestContextFilter.class);

        // CRITICAL: Enable Spring context injection
        property("jersey.config.server.spring.context.inject", true);

        // CRITICAL: Enable automatic Spring bean discovery
        property("jersey.config.server.spring.context.scan", true);

        // CRITICAL: Ensure Jersey looks for Spring-managed beans
        property("jersey.config.server.spring.scope.singleton", true);

        // Register filters and other providers
        register(net.whydah.service.CorsFilter.class);

        // Register Freemarker for MVC
        register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        property(MvcFeature.TEMPLATE_BASE_PATH, "templates");

        // Register HK2 Immediate scope
        register(new ImmediateScopeModule());
        
        //ensure this registered
        register(net.whydah.service.oauth2proxyserver.OAuth2DiscoveryResource.class);
        
        // Enable recursive provider scanning
        property("jersey.config.server.provider.scanning.recursive", true);
    }
}