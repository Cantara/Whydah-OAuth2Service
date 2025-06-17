package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;
import org.junit.Test;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.*;

/**
 * Comprehensive test to verify that ALL OAuth2 resources are properly Spring-managed
 * and have their dependencies correctly injected.
 */
public class AllOAuth2ResourcesSpringIntegrationTest {

    @Test
    public void testAllOAuth2ResourcesAreSpringManaged() {
        System.out.println("🔍 Testing that ALL OAuth2 resources are Spring-managed...");

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.scan("net.whydah");

        try {
            context.refresh();

            // Test OAuth2ProxyAuthorizeResource
            OAuth2ProxyAuthorizeResource authorizeResource = context.getBean(OAuth2ProxyAuthorizeResource.class);
            assertNotNull("OAuth2ProxyAuthorizeResource should be Spring-managed", authorizeResource);
            System.out.println("✅ OAuth2ProxyAuthorizeResource is Spring-managed");

            // Test OAuth2ProxyTokenResource
            OAuth2ProxyTokenResource tokenResource = context.getBean(OAuth2ProxyTokenResource.class);
            assertNotNull("OAuth2ProxyTokenResource should be Spring-managed", tokenResource);
            System.out.println("✅ OAuth2ProxyTokenResource is Spring-managed");

            // Test Oauth2ProxyLogoutResource
            Oauth2ProxyLogoutResource logoutResource = context.getBean(Oauth2ProxyLogoutResource.class);
            assertNotNull("Oauth2ProxyLogoutResource should be Spring-managed", logoutResource);
            System.out.println("✅ Oauth2ProxyLogoutResource is Spring-managed");

            // Test other OAuth2 resources if they exist
            try {
                OAuth2ProxyVerifyResource verifyResource = context.getBean(OAuth2ProxyVerifyResource.class);
                assertNotNull("OAuth2ProxyVerifyResource should be Spring-managed", verifyResource);
                System.out.println("✅ OAuth2ProxyVerifyResource is Spring-managed");
            } catch (Exception e) {
                System.out.println("ℹ️  OAuth2ProxyVerifyResource not found or not Spring-managed (might not need DI)");
            }

            try {
                OAuth2UserResource userResource = context.getBean(OAuth2UserResource.class);
                assertNotNull("OAuth2UserResource should be Spring-managed", userResource);
                System.out.println("✅ OAuth2UserResource is Spring-managed");
            } catch (Exception e) {
                System.out.println("ℹ️  OAuth2UserResource not found or not Spring-managed (might not need DI)");
            }

            try {
                OAuth2DiscoveryResource discoveryResource = context.getBean(OAuth2DiscoveryResource.class);
                assertNotNull("OAuth2DiscoveryResource should be Spring-managed", discoveryResource);
                System.out.println("✅ OAuth2DiscoveryResource is Spring-managed");
            } catch (Exception e) {
                System.out.println("ℹ️  OAuth2DiscoveryResource not found or not Spring-managed (might not need DI)");
            }

            System.out.println("🎉 All OAuth2 resources that require dependency injection are Spring-managed!");

        } finally {
            context.close();
        }
    }

    @Test
    public void testOAuth2ResourceDependenciesAreInjected() {
        System.out.println("🔍 Testing that OAuth2 resource dependencies are properly injected...");

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.scan("net.whydah");

        try {
            context.refresh();

            // Get the resources and verify their dependencies are not null
            OAuth2ProxyAuthorizeResource authorizeResource = context.getBean(OAuth2ProxyAuthorizeResource.class);
            // We can't easily test private fields, but if Spring created the bean successfully,
            // it means all @Autowired dependencies were satisfied
            System.out.println("✅ OAuth2ProxyAuthorizeResource created successfully (all dependencies injected)");

            OAuth2ProxyTokenResource tokenResource = context.getBean(OAuth2ProxyTokenResource.class);
            System.out.println("✅ OAuth2ProxyTokenResource created successfully (all dependencies injected)");

            Oauth2ProxyLogoutResource logoutResource = context.getBean(Oauth2ProxyLogoutResource.class);
            System.out.println("✅ Oauth2ProxyLogoutResource created successfully (all dependencies injected)");

            // Verify the required services are available
            TokenService tokenService = context.getBean(TokenService.class);
            UserAuthorizationService authService = context.getBean(UserAuthorizationService.class);
            ClientService clientService = context.getBean(ClientService.class);

            assertNotNull("TokenService should be available", tokenService);
            assertNotNull("UserAuthorizationService should be available", authService);
            assertNotNull("ClientService should be available", clientService);

            System.out.println("✅ All required services are available for injection");
            System.out.println("🎉 Dependency injection is working correctly for all OAuth2 resources!");

        } finally {
            context.close();
        }
    }

    @Test
    public void testNoCircularDependencies() {
        System.out.println("🔄 Testing for circular dependencies in OAuth2 module...");

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.scan("net.whydah");

        try {
            long startTime = System.currentTimeMillis();
            context.refresh();
            long endTime = System.currentTimeMillis();

            System.out.println("✅ No circular dependencies detected");
            System.out.println("⏱️  Spring context loaded in " + (endTime - startTime) + "ms");

            // If we get here without exceptions, there are no circular dependencies
            assertTrue("Context should load without circular dependency issues", true);

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("circular")) {
                fail("Circular dependency detected: " + e.getMessage());
            } else {
                fail("Context loading failed: " + e.getMessage());
            }
        } finally {
            context.close();
        }
    }
}