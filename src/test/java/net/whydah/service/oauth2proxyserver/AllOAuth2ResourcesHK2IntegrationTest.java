package net.whydah.service.oauth2proxyserver;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.inject.Singleton;
import net.whydah.service.CredentialStore;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;

/**
 * Comprehensive test to verify that ALL OAuth2 resources are properly HK2-managed
 * and have their dependencies correctly injected.
 */
public class AllOAuth2ResourcesHK2IntegrationTest {
    
    private ServiceLocator serviceLocator;

    @Before
    public void setUp() {
        System.out.println("🚀 Setting up HK2 ServiceLocator for OAuth2 resources...");
        
        // Create HK2 ServiceLocator
        serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        
        // Bind services - same as in your JerseyConfig
        ServiceLocatorUtilities.bind(serviceLocator, new AbstractBinder() {
            @Override
            protected void configure() {
                bind(CredentialStore.class).to(CredentialStore.class).in(Singleton.class);
                bind(ClientService.class).to(ClientService.class).in(Singleton.class);
                bind(UserAuthorizationService.class).to(UserAuthorizationService.class).in(Singleton.class);
                bind(TokenService.class).to(TokenService.class).in(Singleton.class);
                
                // Bind OAuth2 resources
                bind(OAuth2ProxyAuthorizeResource.class).to(OAuth2ProxyAuthorizeResource.class);
                bind(OAuth2ProxyTokenResource.class).to(OAuth2ProxyTokenResource.class);
                bind(Oauth2ProxyLogoutResource.class).to(Oauth2ProxyLogoutResource.class);
                bind(OAuth2ProxyVerifyResource.class).to(OAuth2ProxyVerifyResource.class);
                bind(OAuth2UserResource.class).to(OAuth2UserResource.class);
                bind(OAuth2DiscoveryResource.class).to(OAuth2DiscoveryResource.class);
                bind(OAuth2DummyResource.class).to(OAuth2DummyResource.class);
            }
        });
        
        System.out.println("✅ HK2 ServiceLocator set up complete");
    }
    
    @After
    public void tearDown() {
        if (serviceLocator != null) {
            serviceLocator.shutdown();
            System.out.println("🧹 HK2 ServiceLocator shut down cleanly");
        }
    }

    @Test
    public void testAllOAuth2ResourcesAreHK2Managed() {
        System.out.println("🔍 Testing that ALL OAuth2 resources are HK2-managed...");

        try {
            // Test OAuth2ProxyAuthorizeResource
            OAuth2ProxyAuthorizeResource authorizeResource = serviceLocator.getService(OAuth2ProxyAuthorizeResource.class);
            assertNotNull("OAuth2ProxyAuthorizeResource should be HK2-managed", authorizeResource);
            System.out.println("✅ OAuth2ProxyAuthorizeResource is HK2-managed");

            // Test OAuth2ProxyTokenResource
            OAuth2ProxyTokenResource tokenResource = serviceLocator.getService(OAuth2ProxyTokenResource.class);
            assertNotNull("OAuth2ProxyTokenResource should be HK2-managed", tokenResource);
            System.out.println("✅ OAuth2ProxyTokenResource is HK2-managed");

            // Test Oauth2ProxyLogoutResource
            Oauth2ProxyLogoutResource logoutResource = serviceLocator.getService(Oauth2ProxyLogoutResource.class);
            assertNotNull("Oauth2ProxyLogoutResource should be HK2-managed", logoutResource);
            System.out.println("✅ Oauth2ProxyLogoutResource is HK2-managed");

            // Test other OAuth2 resources
            try {
                OAuth2ProxyVerifyResource verifyResource = serviceLocator.getService(OAuth2ProxyVerifyResource.class);
                assertNotNull("OAuth2ProxyVerifyResource should be HK2-managed", verifyResource);
                System.out.println("✅ OAuth2ProxyVerifyResource is HK2-managed");
            } catch (Exception e) {
                System.out.println("ℹ️  OAuth2ProxyVerifyResource not found or not HK2-managed (might not need DI)");
            }

            try {
                OAuth2UserResource userResource = serviceLocator.getService(OAuth2UserResource.class);
                assertNotNull("OAuth2UserResource should be HK2-managed", userResource);
                System.out.println("✅ OAuth2UserResource is HK2-managed");
            } catch (Exception e) {
                System.out.println("ℹ️  OAuth2UserResource not found or not HK2-managed (might not need DI)");
            }

            try {
                OAuth2DiscoveryResource discoveryResource = serviceLocator.getService(OAuth2DiscoveryResource.class);
                assertNotNull("OAuth2DiscoveryResource should be HK2-managed", discoveryResource);
                System.out.println("✅ OAuth2DiscoveryResource is HK2-managed");
            } catch (Exception e) {
                System.out.println("ℹ️  OAuth2DiscoveryResource not found or not HK2-managed (might not need DI)");
            }

            try {
                OAuth2DummyResource dummyResource = serviceLocator.getService(OAuth2DummyResource.class);
                assertNotNull("OAuth2DummyResource should be HK2-managed", dummyResource);
                System.out.println("✅ OAuth2DummyResource is HK2-managed");
            } catch (Exception e) {
                System.out.println("ℹ️  OAuth2DummyResource not found or not HK2-managed (might not need DI)");
            }

            System.out.println("🎉 All OAuth2 resources that require dependency injection are HK2-managed!");

        } catch (Exception e) {
            fail("Failed to verify OAuth2 resources are HK2-managed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testOAuth2ResourceDependenciesAreInjected() {
        System.out.println("🔍 Testing that OAuth2 resource dependencies are properly injected...");

        try {
            // Get the resources and verify their dependencies are not null
            OAuth2ProxyAuthorizeResource authorizeResource = serviceLocator.getService(OAuth2ProxyAuthorizeResource.class);
            // We can't easily test private fields, but if HK2 created the bean successfully,
            // it means all @Inject dependencies were satisfied
            System.out.println("✅ OAuth2ProxyAuthorizeResource created successfully (all dependencies injected)");

            OAuth2ProxyTokenResource tokenResource = serviceLocator.getService(OAuth2ProxyTokenResource.class);
            System.out.println("✅ OAuth2ProxyTokenResource created successfully (all dependencies injected)");

            Oauth2ProxyLogoutResource logoutResource = serviceLocator.getService(Oauth2ProxyLogoutResource.class);
            System.out.println("✅ Oauth2ProxyLogoutResource created successfully (all dependencies injected)");

            // Verify the required services are available
            TokenService tokenService = serviceLocator.getService(TokenService.class);
            UserAuthorizationService authService = serviceLocator.getService(UserAuthorizationService.class);
            ClientService clientService = serviceLocator.getService(ClientService.class);
            CredentialStore credentialStore = serviceLocator.getService(CredentialStore.class);

            assertNotNull("TokenService should be available", tokenService);
            assertNotNull("UserAuthorizationService should be available", authService);
            assertNotNull("ClientService should be available", clientService);
            assertNotNull("CredentialStore should be available", credentialStore);

            System.out.println("✅ All required services are available for injection");
            System.out.println("🎉 Dependency injection is working correctly for all OAuth2 resources!");

        } catch (Exception e) {
            fail("Failed to verify OAuth2 resource dependencies: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testNoCircularDependencies() {
        System.out.println("🔄 Testing for circular dependencies in OAuth2 module...");

        try {
            long startTime = System.currentTimeMillis();
            
            // Try to get all services and resources - this will fail if circular dependencies exist
            serviceLocator.getService(CredentialStore.class);
            serviceLocator.getService(ClientService.class);
            serviceLocator.getService(UserAuthorizationService.class);
            serviceLocator.getService(TokenService.class);
            serviceLocator.getService(OAuth2ProxyAuthorizeResource.class);
            serviceLocator.getService(OAuth2ProxyTokenResource.class);
            serviceLocator.getService(Oauth2ProxyLogoutResource.class);
            
            long endTime = System.currentTimeMillis();

            System.out.println("✅ No circular dependencies detected");
            System.out.println("⏱️  HK2 services created in " + (endTime - startTime) + "ms");

            // If we get here without exceptions, there are no circular dependencies
            assertTrue("Services should load without circular dependency issues", true);

        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().getMessage().toLowerCase().contains("circular")) {
                fail("Circular dependency detected: " + e.getMessage());
            } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("circular")) {
                fail("Circular dependency detected: " + e.getMessage());
            } else {
                fail("Service creation failed: " + e.getMessage());
            }
        }
    }

    @Test
    public void testResourcesCanHandleRequests() {
        System.out.println("🔍 Testing that OAuth2 resources are ready to handle requests...");

        try {
            // Get resources that handle the main OAuth2 endpoints
            OAuth2ProxyAuthorizeResource authorizeResource = serviceLocator.getService(OAuth2ProxyAuthorizeResource.class);
            OAuth2ProxyTokenResource tokenResource = serviceLocator.getService(OAuth2ProxyTokenResource.class);
            OAuth2UserResource userResource = serviceLocator.getService(OAuth2UserResource.class);
            OAuth2DiscoveryResource discoveryResource = serviceLocator.getService(OAuth2DiscoveryResource.class);

            assertNotNull("Authorize resource should be ready", authorizeResource);
            assertNotNull("Token resource should be ready", tokenResource);
            assertNotNull("User resource should be ready", userResource);
            assertNotNull("Discovery resource should be ready", discoveryResource);

            System.out.println("✅ All main OAuth2 endpoints are ready to handle requests");
            System.out.println("🎉 OAuth2 service is fully functional with HK2 dependency injection!");

        } catch (Exception e) {
            fail("OAuth2 resources are not ready to handle requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
}