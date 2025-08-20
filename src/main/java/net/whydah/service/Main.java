package net.whydah.service;

// Imports (same as provided)
import java.util.logging.Level;
import java.util.logging.LogManager;

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
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import net.whydah.config.JerseyConfig;
import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.health.HealthResource;
import net.whydah.service.oauth2proxyserver.OAuth2DiscoveryResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyAuthorizeResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyTokenResource;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyVerifyResource;
import net.whydah.service.oauth2proxyserver.OAuth2UserResource;
import net.whydah.service.oauth2proxyserver.Oauth2ProxyLogoutResource;
import net.whydah.util.Configuration;

public class Main {
    public static final String CONTEXT_PATH = "/oauth2";
    public static final String ROLE_ALL = "allRole";

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static Integer webappPort;
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

        webappPort = Configuration.getInt("service.port");

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

    public void start() {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(CONTEXT_PATH);
        
        webappPort = Configuration.getInt("service.port");
        
        // No Spring context needed - pure HK2
        
        ConstraintSecurityHandler securityHandler = getSecurityHandler();
        context.setSecurityHandler(securityHandler);

        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer());
        jerseyServlet.setInitParameter("jakarta.ws.rs.Application", JerseyConfig.class.getName());
        jerseyServlet.setInitOrder(1);
        
        context.addServlet(jerseyServlet, "/*");

        ServerConnector connector = new ServerConnector(server);
        if (webappPort != null) {
            connector.setPort(webappPort);
        }
        
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
            throw new RuntimeException("Failed to start Jetty server", e);
        }
        
        webappPort = connector.getPort();
        net.whydah.util.Configuration.logProperties();
        log.info("Whydah-OAuth2Service started on http://localhost:{}{}", webappPort, CONTEXT_PATH);
        
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("Jetty server thread interrupted when joining. Pretend everything is OK.", e);
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

        // Add user
        addUser(loginService, Configuration.getString("login.user"), Configuration.getString("login.password"), ROLE_ALL);

        Constraint auth_constraint = new Constraint();
        auth_constraint.setName(Constraint.__BASIC_AUTH);
        auth_constraint.setRoles(new String[]{Constraint.ANY_AUTH, ROLE_ALL});
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