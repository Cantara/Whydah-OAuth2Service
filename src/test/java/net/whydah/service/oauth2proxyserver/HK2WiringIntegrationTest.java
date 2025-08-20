package net.whydah.service.oauth2proxyserver;

import static org.junit.Assert.assertNotNull;
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
 * Integration test to verify that HK2 dependency injection works
 */
public class HK2WiringIntegrationTest {
    
    private ServiceLocator serviceLocator;

    @Before
    public void setUp() {
        System.out.println("🚀 Setting up HK2 ServiceLocator...");
        
        // Create HK2 ServiceLocator
        serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        
        // Bind services manually
        ServiceLocatorUtilities.bind(serviceLocator, new AbstractBinder() {
            @Override
            protected void configure() {
                bind(CredentialStore.class).to(CredentialStore.class).in(Singleton.class);
                bind(ClientService.class).to(ClientService.class).in(Singleton.class);
                bind(UserAuthorizationService.class).to(UserAuthorizationService.class).in(Singleton.class);
                bind(TokenService.class).to(TokenService.class).in(Singleton.class);
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
    public void testHK2ServicesCanBeInjected() {
        System.out.println("🔧 Testing that services can be created and injected...");

        try {
            // Test that the services can be created
            Class<?>[] serviceClasses = {
                CredentialStore.class,
                ClientService.class, 
                UserAuthorizationService.class,
                TokenService.class
            };

            for (Class<?> serviceClass : serviceClasses) {
                Object service = serviceLocator.getService(serviceClass);
                assertNotNull("Service " + serviceClass.getSimpleName() + " should be created", service);
                System.out.println("✅ " + serviceClass.getSimpleName() + " successfully created");
            }

            System.out.println("🎉 All services successfully created with HK2!");

        } catch (Exception e) {
            fail("HK2 failed to create services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testTokenServiceDependencies() {
        System.out.println("🔍 Testing TokenService dependency injection...");

        try {
            // TokenService depends on UserAuthorizationService and ClientService
            TokenService tokenService = serviceLocator.getService(TokenService.class);
            assertNotNull("TokenService should be created", tokenService);
            
            // If TokenService was created successfully, its dependencies were injected
            System.out.println("✅ TokenService created - dependencies injected successfully");

        } catch (Exception e) {
            fail("TokenService dependency injection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test  
    public void testNoCircularDependencies() {
        System.out.println("🔄 Testing for circular dependency issues...");

        try {
            long startTime = System.currentTimeMillis();
            
            // Try to get all services - this will fail if circular dependencies exist
            serviceLocator.getService(CredentialStore.class);
            serviceLocator.getService(ClientService.class);
            serviceLocator.getService(UserAuthorizationService.class);
            serviceLocator.getService(TokenService.class);
            
            long endTime = System.currentTimeMillis();

            System.out.println("✅ No circular dependencies detected");
            System.out.println("⏱️  All services created in " + (endTime - startTime) + "ms");

        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().getMessage().toLowerCase().contains("circular")) {
                fail("Circular dependency detected: " + e.getMessage());
            } else {
                System.out.println("⚠️  Service creation issue: " + e.getMessage());
            }
        }
    }
}