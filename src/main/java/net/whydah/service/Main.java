package net.whydah.service;

import net.whydah.demoservice.oauth2ping.PingResource;
import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.health.HealthResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyAuthorizeResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyTokenResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyVerifyResource;
import net.whydah.util.Configuration;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.ContextLoaderListener;

import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09
 */
public class Main {
    public static final String CONTEXT_PATH = "/Whydah-OAuth2Service";
    public static final String ADMIN_ROLE = "admin";
    public static final String USER_ROLE = "user";

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


        ConstraintSecurityHandler securityHandler = buildSecurityHandler();
        context.setSecurityHandler(securityHandler);

        ResourceConfig jerseyResourceConfig = new ResourceConfig();
        jerseyResourceConfig.packages("net.whydah");
        jerseyResourceConfig.register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        jerseyResourceConfig.property(MvcFeature.TEMPLATE_BASE_PATH, "templates");
//        jerseyResourceConfig.register(MvcFeature.class);
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(jerseyResourceConfig));
        context.addServlet(jerseyServlet, "/*");

        context.addEventListener(new ContextLoaderListener());

        context.setInitParameter("contextConfigLocation", "classpath:context.xml");

        ServerConnector connector = new ServerConnector(server);
        if (webappPort != null) {
            connector.setPort(webappPort);
        }
        NCSARequestLog requestLog = buildRequestLog();
        server.setRequestLog(requestLog);
        server.addConnector(connector);
        server.setHandler(context);

        try {
            server.start();
        } catch (Exception e) {
            log.error("Error during Jetty startup. Exiting", e);
            // "System. exit(2);"
        }
        webappPort = connector.getLocalPort();
        log.info("Whydah-OAuth2Service started on http://localhost:{}{}", webappPort, CONTEXT_PATH);
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("Jetty server thread when join. Pretend everything is OK.", e);
        }
    }

    private NCSARequestLog buildRequestLog() {
        NCSARequestLog requestLog = new NCSARequestLog("logs/jetty-yyyy_mm_dd.request.log");
        requestLog.setAppend(true);
        requestLog.setExtended(true);
        requestLog.setLogTimeZone("GMT");

        return requestLog;
    }

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

        // Allow OAuth2ProxyTokenResource to be accessed without authentication
        ConstraintMapping oauthserverEndpointConstraintMapping = new ConstraintMapping();
        oauthserverEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        oauthserverEndpointConstraintMapping.setPathSpec(OAuth2ProxyTokenResource.OAUTH2TOKENSERVER_PATH);
        securityHandler.addConstraintMapping(oauthserverEndpointConstraintMapping);

        // Allow OAuth2ProxyTokenResource to be accessed without authentication
        ConstraintMapping pingEndpointConstraintMapping = new ConstraintMapping();
        pingEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        pingEndpointConstraintMapping.setPathSpec(PingResource.PING_PATH);
        securityHandler.addConstraintMapping(pingEndpointConstraintMapping);

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

        //TODO fix login flow
        // Allow userAuthorization to be accessed without authentication
        ConstraintMapping userAuthorization = new ConstraintMapping();
        userAuthorization.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        userAuthorization.setPathSpec(UserAuthorizationResource.USER_PATH + "/*");
        securityHandler.addConstraintMapping(userAuthorization);

        HashLoginService loginService = new HashLoginService("Whydah-OAuth2Service");

        String clientUsername = Configuration.getString("login.user");
        String clientPassword = Configuration.getString("login.password");
        loginService.putUser(clientUsername, new Password(clientPassword), new String[]{USER_ROLE});

        String adminUsername = Configuration.getString("login.admin.user");
        String adminPassword = Configuration.getString("login.admin.password");
        loginService.putUser(adminUsername, new Password(adminPassword), new String[]{ADMIN_ROLE});

        log.debug("Main instantiated with basic auth clientuser={} and adminuser={}", clientUsername, adminUsername);
        securityHandler.setLoginService(loginService);
        return securityHandler;
    }


    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.warn("Error when stopping Jetty server", e);
        }
    }

    public int getPort() {
        return webappPort;
    }

    public boolean isStarted() {
        return server.isStarted();
    }
}