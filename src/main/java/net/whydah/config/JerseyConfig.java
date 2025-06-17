package net.whydah.config;

import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;
import net.whydah.service.oauth2proxyserver.TokenService;
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

        // CRITICAL: Enable Spring context injection FIRST
        property("jersey.config.server.spring.context.inject", true);

        // CRITICAL: Enable automatic Spring bean discovery
        property("jersey.config.server.spring.context.scan", true);

        // Let Spring manage all resources via package scanning
        packages("net.whydah.service");

        // Manual registration of key services (fallback if auto-discovery fails)
        register(TokenService.class);
        register(UserAuthorizationService.class);
        register(ClientService.class);


        // Register filters and other providers
        register(net.whydah.service.CorsFilter.class);

        // Register Freemarker for MVC
        register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        property(MvcFeature.TEMPLATE_BASE_PATH, "templates");

        // Let Jersey discover Spring-managed resources
        property("jersey.config.server.provider.scanning.recursive", true);

        // IMPORTANT: Make sure Jersey knows to look for Spring-managed beans
        property("jersey.config.server.spring.scope.singleton", false);
    }
}