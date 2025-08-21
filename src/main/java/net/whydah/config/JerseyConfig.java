package net.whydah.config;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;

import ch.qos.logback.core.net.server.Client;
import jakarta.inject.Singleton;
import net.whydah.service.CredentialStore;
import net.whydah.service.authorizations.SSOUserSessionRepository;
import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.authorizations.UserAuthorizationsRepository;
import net.whydah.service.clients.ClientRepository;
import net.whydah.service.clients.ClientService;
import net.whydah.service.health.HealthResource;
import net.whydah.service.oauth2proxyserver.OAuth2DiscoveryResource;
import net.whydah.service.oauth2proxyserver.OAuth2DummyResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyAuthorizeResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyTokenResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyVerifyResource;
import net.whydah.service.oauth2proxyserver.OAuth2UserResource;
import net.whydah.service.oauth2proxyserver.Oauth2ProxyLogoutResource;
import net.whydah.service.oauth2proxyserver.TokenService;

public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // Package scanning for JAX-RS resources and providers
        packages("net.whydah.service");
        
        // MVC and other features
        register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        property(MvcFeature.TEMPLATE_BASE_PATH, "templates");
        
        // CORS filter
        register(net.whydah.service.CorsFilter.class);
        
        // Register HK2 binder for dependency injection
        register(new ServiceBinder());
        
        // Explicitly register JAX-RS resources
        register(HealthResource.class);
        register(OAuth2DiscoveryResource.class);
        register(OAuth2DummyResource.class);
        register(OAuth2ProxyAuthorizeResource.class);
        register(Oauth2ProxyLogoutResource.class);
        register(OAuth2ProxyTokenResource.class);
        register(OAuth2ProxyVerifyResource.class);
        register(OAuth2UserResource.class);
        register(UserAuthorizationResource.class);
    }
    
    private static class ServiceBinder extends AbstractBinder {
        @Override
        protected void configure() {
            // Bind services to HK2
            bind(CredentialStore.class).to(CredentialStore.class).in(Singleton.class);
            bind(ClientService.class).to(ClientService.class).in(Singleton.class);
            bind(UserAuthorizationService.class).to(UserAuthorizationService.class).in(Singleton.class);
            bind(TokenService.class).to(TokenService.class).in(Singleton.class);
            bind(ClientRepository.class).to(ClientRepository.class).in(Singleton.class);
            bind(SSOUserSessionRepository.class).to(SSOUserSessionRepository.class).in(Singleton.class);
            bind(UserAuthorizationsRepository.class).to(UserAuthorizationsRepository.class).in(Singleton.class);
        }
    }
}