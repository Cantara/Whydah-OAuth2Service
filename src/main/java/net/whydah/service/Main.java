package net.whydah.service;

import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.health.HealthResource;
import net.whydah.service.oauth2proxyserver.*;
import net.whydah.util.Configuration;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09
 */
public class Main {
    public static final String CONTEXT_PATH = "/oauth2";
    public static final String ROLE_ALL = "allRole";

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Integer webappPort;
    private Server server;


    public Main() {
        this.server = new Server();
    }

    public Main withPort(Integer webappPort) {
        this.webappPort = webappPort;
        return this;
    }

    public static void main(String[] args) {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

        Integer webappPort = Configuration.getInt("service.port");

        try {

            final Main main = new Main().withPort(webappPort);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.debug("ShutdownHook triggered. Exiting Whydah-OAuth2Service");
                    main.stop();
                }
            });

            main.start();
            log.debug("Finished waiting for Thread.currentThread().join()");
            main.stop();
        } catch (RuntimeException e) {
            log.error("Error during startup. Shutting down Whydah-OAuth2Service.", e);
            System.exit(1);
        }
    }

    // https://github.com/psamsotha/jersey-spring-jetty/blob/master/src/main/java/com/underdog/jersey/spring/jetty/JettyServerMain.java
    public void start() {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(CONTEXT_PATH);

        // Add this: Set up the Spring application context
        // context.addEventListener(new org.springframework.web.context.ContextLoaderListener());


        ConstraintSecurityHandler securityHandler = getSecurityHandler();
        context.setSecurityHandler(securityHandler);

        ResourceConfig jerseyResourceConfig = new ResourceConfig();
        jerseyResourceConfig.packages("net.whydah");
        jerseyResourceConfig.register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        jerseyResourceConfig.property(MvcFeature.TEMPLATE_BASE_PATH, "templates");

        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(jerseyResourceConfig));
        context.addServlet(jerseyServlet, "/*");

//        context.addEventListener(new ContextLoaderListener());
// With the proper ContextLoaderListener
        context.addEventListener(new org.springframework.web.context.ContextLoaderListener());

        context.setInitParameter("contextConfigLocation", "classpath:context.xml");

        ServerConnector connector = new ServerConnector(server);
        if (webappPort != null) {
            connector.setPort(webappPort);
        }
//        NCSARequestLog requestLog = buildRequestLog();
        RequestLogWriter logWriter = new RequestLogWriter("logs/jetty-yyyy_mm_dd.request.log");
        logWriter.setAppend(true);
        logWriter.setTimeZone("GMT");
        CustomRequestLog requestLog = new CustomRequestLog(logWriter, CustomRequestLog.EXTENDED_NCSA_FORMAT);
        server.setRequestLog(requestLog);
        server.addConnector(connector);
        server.setHandler(context);

        try {
            server.start();
        } catch (Exception e) {
        	e.printStackTrace();
            log.error("Error during Jetty startup. Exiting {}", e);
            // "System. exit(2);"
        }
        webappPort = connector.getLocalPort();


        // Log the configuration right before announcing the service is started
        net.whydah.util.Configuration.logProperties();
        log.info("Whydah-OAuth2Service started on http://localhost:{}{}", webappPort, CONTEXT_PATH);
        
       
        
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("Jetty server thread when join. Pretend everything is OK.", e);
        }
    }


    private String realm = "whydah-oauth2";


    private ConstraintSecurityHandler getSecurityHandler() {
    	HashLoginService loginService = new HashLoginService();
    	loginService.setName(realm);
    	ConstraintSecurityHandler handler = new ConstraintSecurityHandler();
    	handler.setAuthenticator(new BasicAuthenticator());
    	handler.setRealmName(realm);
    	handler.setLoginService(loginService);
    	
    	//add user
    	addUser(loginService,  Configuration.getString("login.user"),  Configuration.getString("login.password"), ROLE_ALL);


        Constraint auth_constraint = new Constraint();
    	auth_constraint.setName(Constraint.__BASIC_AUTH);
    	auth_constraint.setRoles(new String[] {Constraint.ANY_AUTH, ROLE_ALL});
    	auth_constraint.setAuthenticate(true);

        Constraint no_constraint = new Constraint(Constraint.NONE, Constraint.ANY_ROLE);


    	addConstraintMapping(handler, "/*", auth_constraint);
    	addConstraintMapping(handler, "/images/*", no_constraint);
    	addConstraintMapping(handler, "/css/*", no_constraint);
    	addConstraintMapping(handler, HealthResource.HEALTH_PATH, no_constraint);
    	addConstraintMapping(handler, OAuth2DiscoveryResource.OAUTH2DISCOVERY_PATH + "/*", no_constraint);
    	addConstraintMapping(handler, OAuth2ProxyTokenResource.OAUTH2TOKENSERVER_PATH, no_constraint);
    	addConstraintMapping(handler, OAuth2ProxyVerifyResource.OAUTH2TOKENVERIFY_PATH, no_constraint);
    	addConstraintMapping(handler, OAuth2ProxyAuthorizeResource.OAUTH2AUTHORIZE_PATH + "/*", no_constraint);
    	addConstraintMapping(handler, OAuth2UserResource.OAUTH2USERINFO_PATH, no_constraint);
    	addConstraintMapping(handler, UserAuthorizationResource.USER_PATH + "/*", no_constraint);
    	addConstraintMapping(handler, Oauth2ProxyLogoutResource.OAUTH2LOGOUT_PATH + "/*", no_constraint);
    	
		return handler;
    }
    
    public void addUser(HashLoginService loginService, String userId, String password, String... roles) {
    	UserStore userStore = new UserStore();
    	userStore.addUser(userId, Credential.getCredential(password), roles);
    	loginService.setUserStore(userStore);
    }

    
    private void addConstraintMapping(ConstraintSecurityHandler handler, String path, Constraint constraint) {
    	ConstraintMapping cm = new ConstraintMapping();
    	cm.setPathSpec(path);
    	cm.setConstraint(constraint);
    	handler.addConstraintMapping(cm);
    }

    /*
    private ConstraintSecurityHandler buildSecurityHandler() {
        Constraint userRoleConstraint = new Constraint();
        userRoleConstraint.setName(Constraint.__BASIC_AUTH);
        userRoleConstraint.setRoles(new String[]{USER_ROLE, ADMIN_ROLE});
        userRoleConstraint.setAuthenticate(true);

        Constraint adminRoleConstraint = new Constraint();
        adminRoleConstraint.setName(Constraint.__BASIC_AUTH);
        adminRoleConstraint.setRoles(new String[]{ADMIN_ROLE});
        adminRoleConstraint.setAuthenticate(true);

        ConstraintMapping clientConstraintMapping = new ConstraintMapping();
        clientConstraintMapping.setConstraint(userRoleConstraint);
        clientConstraintMapping.setPathSpec("/client/*");

        ConstraintMapping adminRoleConstraintMapping = new ConstraintMapping();
        adminRoleConstraintMapping.setConstraint(adminRoleConstraint);
        adminRoleConstraintMapping.setPathSpec("/*");

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.addConstraintMapping(clientConstraintMapping);
        securityHandler.addConstraintMapping(adminRoleConstraintMapping);

        // Allow healthresource to be accessed without authentication
        ConstraintMapping healthEndpointConstraintMapping = new ConstraintMapping();
        healthEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        healthEndpointConstraintMapping.setPathSpec(HealthResource.HEALTH_PATH);
        securityHandler.addConstraintMapping(healthEndpointConstraintMapping);
        
        // Allow OAuth2DummyResource to be accessed with a bsaic authentication. This resource provides a dummy JWT access token
        ConstraintMapping oauth2DummyEndpointConstraintMapping = new ConstraintMapping();
        oauth2DummyEndpointConstraintMapping.setConstraint(userRoleConstraint);
        oauth2DummyEndpointConstraintMapping.setPathSpec(OAuth2DummyResource.OAUTH2DUMMY_PATH);
        securityHandler.addConstraintMapping(oauth2DummyEndpointConstraintMapping);

        
        ConstraintMapping discoveryEndpointConstraintMapping = new ConstraintMapping();
        discoveryEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        discoveryEndpointConstraintMapping.setPathSpec(OAuth2DiscoveryResource.OAUTH2DISCOVERY_PATH + "/*");
        securityHandler.addConstraintMapping(discoveryEndpointConstraintMapping);

        // Allow OAuth2ProxyTokenResource to be accessed without authentication
        ConstraintMapping oauthserverEndpointConstraintMapping = new ConstraintMapping();
        oauthserverEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        oauthserverEndpointConstraintMapping.setPathSpec(OAuth2ProxyTokenResource.OAUTH2TOKENSERVER_PATH);
        securityHandler.addConstraintMapping(oauthserverEndpointConstraintMapping);

        // Allow tokenverifyerResource to be accessed without authentication
        ConstraintMapping tokenVerifyConstraintMapping = new ConstraintMapping();
        tokenVerifyConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        tokenVerifyConstraintMapping.setPathSpec(OAuth2ProxyVerifyResource.OAUTH2TOKENVERIFY_PATH);
        securityHandler.addConstraintMapping(tokenVerifyConstraintMapping);

        // Allow tokenverifyerResource to be accessed without authentication
        ConstraintMapping authorizeConstraintMapping = new ConstraintMapping();
        authorizeConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        authorizeConstraintMapping.setPathSpec(OAuth2ProxyAuthorizeResource.OAUTH2AUTHORIZE_PATH + "/*");
        securityHandler.addConstraintMapping(authorizeConstraintMapping);
        
       // Allow userinfoResource to be accessed without authentication
        ConstraintMapping userInfoConstraintMapping = new ConstraintMapping();
        userInfoConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        userInfoConstraintMapping.setPathSpec(OAuth2UserResource.OAUTH2USERINFO_PATH);
        securityHandler.addConstraintMapping(userInfoConstraintMapping);

        ConstraintMapping userAuthorization = new ConstraintMapping();
        userAuthorization.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        userAuthorization.setPathSpec(UserAuthorizationResource.USER_PATH + "/*");
        securityHandler.addConstraintMapping(userAuthorization);
        
        //TODO fix login flow
        // Allow userAuthorization to be accessed without authentication
        ConstraintMapping logout= new ConstraintMapping();
        logout.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        logout.setPathSpec(Oauth2ProxyLogoutResource.OAUTH2LOGOUT_PATH + "/*");
        securityHandler.addConstraintMapping(logout);

        HashLoginService loginService = new HashLoginService("Whydah-OAuth2Service");

        String clientUsername = Configuration.getString("login.user");
        String clientPassword = Configuration.getString("login.password");
        UserStore userStore = new UserStore();
        userStore.addUser(clientUsername, new Password(clientPassword), new String[]{USER_ROLE});

        String adminUsername = Configuration.getString("login.admin.user");
        String adminPassword = Configuration.getString("login.admin.password");
        userStore.addUser(adminUsername, new Password(adminPassword), new String[]{ADMIN_ROLE});
        loginService.setUserStore(userStore);

        log.debug("Main instantiated with basic auth clientuser={} and adminuser={}", clientUsername, adminUsername);
        securityHandler.setLoginService(loginService);
        return securityHandler;
    }*/


    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.warn("Error when stopping Jetty server", e);
        }
    }

    public int getPort() {
        if (webappPort == null) {
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                log.error("Interrupted while waiting for jetty to start", e);
            }
        }
        return webappPort;
    }

    public boolean isStarted() {
        return server.isStarted();
    }
}