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
 * Test that reproduces and verifies HK2 dependency injection for OAuth2 resources.
 * This test validates that all services are properly bound and can be resolved by HK2.
 */
public class OAuth2ResourceDependencyInjectionFailureTest {
    
    private ServiceLocator serviceLocator;

    @Before
    public void setUp() {
        System.out.println("🚀 Setting up HK2 ServiceLocator for dependency injection test...");
        
        // Create HK2 ServiceLocator
        serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        
        // Bind services - same as in JerseyConfig
        ServiceLocatorUtilities.bind(serviceLocator, new TestServiceBinder());
        
        System.out.println("✅ HK2 ServiceLocator set up complete");
    }
    
    @After
    public void tearDown() {
        if (serviceLocator != null) {
            serviceLocator.shutdown();
            System.out.println("🧹 HK2 ServiceLocator shut down cleanly");
        }
    }
    
    private static class TestServiceBinder extends AbstractBinder {
        @Override
        protected void configure() {
            // Bind all required services
            bind(CredentialStore.class).to(CredentialStore.class).in(Singleton.class);
            bind(ClientService.class).to(ClientService.class).in(Singleton.class);
            bind(UserAuthorizationService.class).to(UserAuthorizationService.class).in(Singleton.class);
            bind(TokenService.class).to(TokenService.class).in(Singleton.class);
            
            // Bind the OAuth2 resource
            bind(OAuth2ProxyAuthorizeResource.class).to(OAuth2ProxyAuthorizeResource.class);
        }
    }

    @Test
    public void testOAuth2ProxyAuthorizeResourceIsHK2Managed() {
        System.out.println("🔍 Testing if OAuth2ProxyAuthorizeResource can be created by HK2...");

        try {
            // Try to get OAuth2ProxyAuthorizeResource from HK2 context
            OAuth2ProxyAuthorizeResource resource = serviceLocator.getService(OAuth2ProxyAuthorizeResource.class);
            
            if (resource != null) {
                System.out.println("✅ SUCCESS: OAuth2ProxyAuthorizeResource IS HK2-managed!");
                System.out.println("✅ This means dependency injection worked correctly!");
                assertNotNull("Resource should be available from HK2 context", resource);
            } else {
                System.out.println("❌ FAILURE: OAuth2ProxyAuthorizeResource could not be created by HK2");
                fail("OAuth2ProxyAuthorizeResource should be creatable by HK2 with proper service binding");
            }

        } catch (Exception e) {
            System.out.println("❌ EXPECTED FAILURE: OAuth2ProxyAuthorizeResource HK2 creation failed");
            System.out.println("❌ Error: " + e.getMessage());
            System.out.println("❌ This indicates HK2 dependency injection issues!");

            // Check if it's a dependency injection related error
            String errorMsg = e.getMessage();
            if (errorMsg != null && 
                (errorMsg.contains("UnsatisfiedDependencyException") ||
                 errorMsg.contains("MultiException") ||
                 errorMsg.contains("Unable to create") ||
                 errorMsg.contains("injection"))) {
                
                fail("HK2 dependency injection failed! " +
                     "Check that all services are properly bound in ServiceBinder. " +
                     "Error: " + errorMsg);
            } else {
                fail("Unexpected error creating OAuth2ProxyAuthorizeResource: " + errorMsg);
            }
        }
    }

    @Test
    public void testRequiredServicesAreHK2Managed() {
        System.out.println("🔍 Verifying that required services ARE HK2-managed...");

        try {
            // These services SHOULD be available in HK2 context
            TokenService tokenService = serviceLocator.getService(TokenService.class);
            assertNotNull("TokenService should be HK2-managed", tokenService);
            System.out.println("✅ TokenService is HK2-managed");

            UserAuthorizationService authService = serviceLocator.getService(UserAuthorizationService.class);
            assertNotNull("UserAuthorizationService should be HK2-managed", authService);
            System.out.println("✅ UserAuthorizationService is HK2-managed");

            ClientService clientService = serviceLocator.getService(ClientService.class);
            assertNotNull("ClientService should be HK2-managed", clientService);
            System.out.println("✅ ClientService is HK2-managed");
            
            CredentialStore credentialStore = serviceLocator.getService(CredentialStore.class);
            assertNotNull("CredentialStore should be HK2-managed", credentialStore);
            System.out.println("✅ CredentialStore is HK2-managed");

            System.out.println("🎯 All required services are HK2-managed!");
            System.out.println("🎯 OAuth2ProxyAuthorizeResource should be able to get these dependencies!");

        } catch (Exception e) {
            fail("Required services should be HK2-managed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testHK2CanResolveAllDependencies() {
        System.out.println("🔍 Testing HK2's ability to resolve all dependencies...");

        try {
            Class<?> resourceClass = OAuth2ProxyAuthorizeResource.class;
            var constructors = resourceClass.getConstructors();
            
            System.out.println("Available constructors: " + constructors.length);
            
            // Find the @Inject constructor
            var injectConstructor = constructors[0]; // Should be the @Inject constructor
            
            System.out.println("Constructor parameter count: " + injectConstructor.getParameterCount());
            System.out.println("Parameter types: ");
            for (var paramType : injectConstructor.getParameterTypes()) {
                System.out.println("  - " + paramType.getSimpleName());
                
                // Test that HK2 can resolve each parameter type
                Object service = serviceLocator.getService(paramType);
                if (service != null) {
                    System.out.println("    ✅ HK2 can resolve " + paramType.getSimpleName());
                } else {
                    System.out.println("    ❌ HK2 CANNOT resolve " + paramType.getSimpleName());
                    fail("HK2 cannot resolve required dependency: " + paramType.getSimpleName());
                }
            }

            // If we get here, all dependencies can be resolved
            System.out.println("🎯 HK2 can resolve all required dependencies!");
            
            // Now test actual resource creation
            OAuth2ProxyAuthorizeResource resource = serviceLocator.getService(OAuth2ProxyAuthorizeResource.class);
            assertNotNull("Resource should be created with all dependencies resolved", resource);
            System.out.println("✅ OAuth2ProxyAuthorizeResource created successfully with all dependencies!");

        } catch (Exception e) {
            System.out.println("❌ HK2 dependency resolution failed: " + e.getMessage());
            e.printStackTrace();
            fail("HK2 should be able to resolve all dependencies: " + e.getMessage());
        }
    }
    
    @Test
    public void testServiceDependencyChain() {
        System.out.println("🔍 Testing service dependency chain...");

        try {
            // Test that TokenService can be created (it depends on UserAuthorizationService and ClientService)
            TokenService tokenService = serviceLocator.getService(TokenService.class);
            assertNotNull("TokenService should be created", tokenService);
            System.out.println("✅ TokenService created - its dependencies were resolved");

            // Test that UserAuthorizationService can be created
            UserAuthorizationService userAuthService = serviceLocator.getService(UserAuthorizationService.class);
            assertNotNull("UserAuthorizationService should be created", userAuthService);
            System.out.println("✅ UserAuthorizationService created");

            // Test that ClientService can be created
            ClientService clientService = serviceLocator.getService(ClientService.class);
            assertNotNull("ClientService should be created", clientService);
            System.out.println("✅ ClientService created");

            // Test that CredentialStore can be created
            CredentialStore credentialStore = serviceLocator.getService(CredentialStore.class);
            assertNotNull("CredentialStore should be created", credentialStore);
            System.out.println("✅ CredentialStore created");

            System.out.println("🎯 Complete dependency chain works in HK2!");

        } catch (Exception e) {
            System.out.println("❌ Service dependency chain failed: " + e.getMessage());
            e.printStackTrace();
            fail("Service dependency chain should work: " + e.getMessage());
        }
    }
    
    @Test
    public void testMultipleResourcesWithSameDependencies() {
        System.out.println("🔍 Testing multiple OAuth2 resources with shared dependencies...");

        try {
            // Test multiple resources that all depend on the same services
            OAuth2ProxyAuthorizeResource authorizeResource = serviceLocator.getService(OAuth2ProxyAuthorizeResource.class);
            assertNotNull("OAuth2ProxyAuthorizeResource should be created", authorizeResource);
            System.out.println("✅ OAuth2ProxyAuthorizeResource created");

            OAuth2ProxyTokenResource tokenResource = serviceLocator.getService(OAuth2ProxyTokenResource.class);
            assertNotNull("OAuth2ProxyTokenResource should be created", tokenResource);
            System.out.println("✅ OAuth2ProxyTokenResource created");

            Oauth2ProxyLogoutResource logoutResource = serviceLocator.getService(Oauth2ProxyLogoutResource.class);
            assertNotNull("Oauth2ProxyLogoutResource should be created", logoutResource);
            System.out.println("✅ Oauth2ProxyLogoutResource created");

            System.out.println("🎯 All OAuth2 resources can be created with shared dependencies!");

        } catch (Exception e) {
            System.out.println("❌ Multiple resource creation failed: " + e.getMessage());
            e.printStackTrace();
            fail("Multiple resources should be creatable: " + e.getMessage());
        }
    }
}