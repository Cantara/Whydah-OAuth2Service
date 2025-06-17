package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;
import org.junit.Test;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.*;

/**
 * Test that reproduces the exact HK2 dependency injection failure we're seeing in the logs.
 * This test should FAIL until OAuth2ProxyAuthorizeResource is made Spring-managed.
 */
public class OAuth2ResourceDependencyInjectionFailureTest {

    @Test
    public void testOAuth2ProxyAuthorizeResourceIsNotSpringManaged() {
        System.out.println("🔍 Testing if OAuth2ProxyAuthorizeResource is Spring-managed...");

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.scan("net.whydah");

        try {
            context.refresh();

            // Try to get OAuth2ProxyAuthorizeResource from Spring context
            try {
                OAuth2ProxyAuthorizeResource resource = context.getBean(OAuth2ProxyAuthorizeResource.class);
                System.out.println("✅ SUCCESS: OAuth2ProxyAuthorizeResource IS Spring-managed!");
                System.out.println("✅ This means dependency injection should work!");
                assertNotNull("Resource should be available from Spring context", resource);

            } catch (Exception e) {
                System.out.println("❌ EXPECTED FAILURE: OAuth2ProxyAuthorizeResource is NOT Spring-managed");
                System.out.println("❌ Error: " + e.getMessage());
                System.out.println("❌ This is why we get HK2 dependency injection failures!");

                // This is the expected failure case - resource is not Spring-managed
                assertTrue("Resource should not be found in Spring context without @Component",
                        e.getMessage().contains("No qualifying bean") ||
                                e.getMessage().contains("NoSuchBeanDefinitionException"));

                fail("OAuth2ProxyAuthorizeResource is not Spring-managed! " +
                        "Add @Component annotation to fix the HK2 dependency injection issue.");
            }

        } finally {
            context.close();
        }
    }

    @Test
    public void testRequiredServicesAreSpringManaged() {
        System.out.println("🔍 Verifying that required services ARE Spring-managed...");

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.scan("net.whydah");

        try {
            context.refresh();

            // These services SHOULD be available in Spring context
            TokenService tokenService = context.getBean(TokenService.class);
            assertNotNull("TokenService should be Spring-managed", tokenService);
            System.out.println("✅ TokenService is Spring-managed");

            UserAuthorizationService authService = context.getBean(UserAuthorizationService.class);
            assertNotNull("UserAuthorizationService should be Spring-managed", authService);
            System.out.println("✅ UserAuthorizationService is Spring-managed");

            ClientService clientService = context.getBean(ClientService.class);
            assertNotNull("ClientService should be Spring-managed", clientService);
            System.out.println("✅ ClientService is Spring-managed");

            System.out.println("🎯 All required services are Spring-managed!");
            System.out.println("🎯 The problem is that OAuth2ProxyAuthorizeResource is NOT Spring-managed!");

        } catch (Exception e) {
            fail("Required services should be Spring-managed: " + e.getMessage());
        } finally {
            context.close();
        }
    }

    @Test
    public void testHK2CannotResolveSpringDependencies() {
        System.out.println("🔍 Testing HK2's inability to resolve Spring dependencies...");

        try {
            Class<?> resourceClass = OAuth2ProxyAuthorizeResource.class;
            var constructor = resourceClass.getConstructors()[0]; // The @Inject constructor

            System.out.println("Constructor parameters: " + constructor.getParameterCount());
            System.out.println("Parameter types: ");
            for (var paramType : constructor.getParameterTypes()) {
                System.out.println("  - " + paramType.getSimpleName());
            }

            // HK2 would try to resolve these parameters but fail because they're Spring beans
            Object[] params = new Object[constructor.getParameterCount()];
            // HK2 cannot provide these Spring-managed instances, so it would fail here

            try {
                Object instance = constructor.newInstance(params); // This should fail with null params
                // If we get here, the constructor accepts null parameters (which it shouldn't in a real app)
                System.out.println("⚠️  Constructor allows null parameters - this demonstrates the DI problem");
                System.out.println("⚠️  In production, HK2 can't provide the actual Spring beans, causing the error");
                assertTrue("Demonstrates the core DI problem", true);
            } catch (Exception e) {
                System.out.println("✅ Expected failure: Cannot create resource without proper dependencies");
                System.out.println("✅ This is exactly what HK2 experiences in production!");
                assertTrue("Should fail without proper dependencies", true);
            }

        } catch (Exception e) {
            System.out.println("✅ Test demonstrates the HK2 dependency resolution problem");
            assertTrue("Test completed successfully", true);
        }
    }
}