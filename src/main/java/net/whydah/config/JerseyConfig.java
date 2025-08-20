package net.whydah.config;

import org.glassfish.hk2.utilities.ImmediateScopeModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.context.annotation.Configuration;

import net.whydah.service.CredentialStore;
import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;
import net.whydah.service.oauth2proxyserver.OAuth2DiscoveryResource;
import net.whydah.service.oauth2proxyserver.OAuth2DummyResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyAuthorizeResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyTokenResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyVerifyResource;
import net.whydah.service.oauth2proxyserver.OAuth2UserResource;
import net.whydah.service.oauth2proxyserver.Oauth2ProxyLogoutResource;
import net.whydah.service.oauth2proxyserver.TokenService;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
    	// Let Spring manage all resources via package scanning
        packages("net.whydah.service");
        
      

        // CRITICAL: Enable Spring context injection
        property("jersey.config.server.spring.context.inject", true);

        // CRITICAL: Enable automatic Spring bean discovery
        property("jersey.config.server.spring.context.scan", true);

        // CRITICAL: Ensure Jersey looks for Spring-managed beans
        property("jersey.config.server.spring.scope.singleton", true);

        
        // Enable recursive provider scanning
        property("jersey.config.server.provider.scanning.recursive", true);
        
        // CRITICAL: Register Spring integration components FIRST
        register(SpringLifecycleListener.class);
        register(RequestContextFilter.class);
        register(net.whydah.service.CorsFilter.class);

        // Register Freemarker for MVC
        register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        property(MvcFeature.TEMPLATE_BASE_PATH, "templates");

        // Register HK2 Immediate scope
        register(new ImmediateScopeModule());
        
        //ensure this registered
        // Explicitly register resources and services
        register(OAuth2DiscoveryResource.class);
        register(OAuth2DummyResource.class);
        register(OAuth2ProxyAuthorizeResource.class);
        register(Oauth2ProxyLogoutResource.class);
        register(OAuth2ProxyTokenResource.class);
        register(OAuth2ProxyVerifyResource.class);
        register(OAuth2UserResource.class);
        register(UserAuthorizationResource.class);
        register(TokenService.class);
        register(UserAuthorizationService.class);
        register(ClientService.class);
        register(CredentialStore.class);
        
        
        
       
    }
}