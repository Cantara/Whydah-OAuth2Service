package net.whydah.service.oauth2proxyserver;

import net.whydah.service.CredentialStore;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simple test to verify @Inject constructors work
 */
public class OAuth2DependencyInjectionTest {

    @Test
    public void testInjectConstructorsExist() {
        // This test verifies that the @Inject constructors exist and have the right signature
        // If the constructors don't exist or have wrong parameters, this will fail at compile time

        Class<?>[] logoutConstructorParams = {UserAuthorizationService.class, ClientService.class};
        Class<?>[] authorizeConstructorParams = {TokenService.class, UserAuthorizationService.class, ClientService.class};
        Class<?>[] tokenConstructorParams = {CredentialStore.class, TokenService.class, ClientService.class, UserAuthorizationService.class};

        try {
            // Test that constructors exist
            Oauth2ProxyLogoutResource.class.getConstructor(logoutConstructorParams);
            System.out.println("✅ Oauth2ProxyLogoutResource @Inject constructor exists");

            OAuth2ProxyAuthorizeResource.class.getConstructor(authorizeConstructorParams);
            System.out.println("✅ OAuth2ProxyAuthorizeResource @Inject constructor exists");

            OAuth2ProxyTokenResource.class.getConstructor(tokenConstructorParams);
            System.out.println("✅ OAuth2ProxyTokenResource @Inject constructor exists");

            System.out.println("🎉 All @Inject constructors are properly defined!");

        } catch (NoSuchMethodException e) {
            fail("Constructor not found: " + e.getMessage());
        }
    }

    @Test
    public void testConstructorAnnotations() {
        // This test checks that the constructors are properly annotated
        try {
            // Get the constructors
            var logoutConstructor = Oauth2ProxyLogoutResource.class.getConstructor(
                    UserAuthorizationService.class, ClientService.class);
            var authorizeConstructor = OAuth2ProxyAuthorizeResource.class.getConstructor(
                    TokenService.class, UserAuthorizationService.class, ClientService.class);
            var tokenConstructor = OAuth2ProxyTokenResource.class.getConstructor(
                    CredentialStore.class, TokenService.class, ClientService.class, UserAuthorizationService.class);

            // Check if @Inject annotation is present
            boolean logoutHasInject = logoutConstructor.isAnnotationPresent(jakarta.inject.Inject.class);
            boolean authorizeHasInject = authorizeConstructor.isAnnotationPresent(jakarta.inject.Inject.class);
            boolean tokenHasInject = tokenConstructor.isAnnotationPresent(jakarta.inject.Inject.class);

            assertTrue("Logout constructor should have @Inject", logoutHasInject);
            assertTrue("Authorize constructor should have @Inject", authorizeHasInject);
            assertTrue("Token constructor should have @Inject", tokenHasInject);

            System.out.println("✅ All constructors have @Inject annotation");

        } catch (Exception e) {
            fail("Error checking annotations: " + e.getMessage());
        }
    }
}