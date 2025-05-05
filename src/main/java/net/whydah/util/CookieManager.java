package net.whydah.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.whydah.commands.config.ConstantValues;
import net.whydah.sso.ddd.model.user.UserTokenId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class CookieManager {
	public static String USER_TOKEN_REFERENCE_NAME = "whydahusertoken_sso";
    //private static final String LOGOUT_COOKIE_VALUE = "logout";
    private static final Logger log = LoggerFactory.getLogger(CookieManager.class);
    private static final int DEFAULT_COOKIE_MAX_AGE = 365 * 24 * 60 * 60;

    private static String cookiedomain = null;
    private static String MY_APP_URI;
    private static boolean IS_MY_URI_SECURED = false;

    private CookieManager() {
    }

    static {
        try {
            cookiedomain = Configuration.getString("whydah.cookiedomain");
            MY_APP_URI = Configuration.getString("myuri");
            String cookiename = Configuration.getString("cookiename");
            //some overrides
            URL uri;
        	if(MY_APP_URI!=null){
            
				 uri = new URL(MY_APP_URI);
				 IS_MY_URI_SECURED = MY_APP_URI.indexOf("https") >= 0;
				 if(cookiedomain==null || cookiedomain.isEmpty()){
					 String domain = uri.getHost();
					 domain = domain.startsWith("www.") ? domain.substring(4) : domain;
					 cookiedomain = domain;
				 }
			 }
        	
        	if(cookiename!=null && !cookiename.isEmpty()) {
        		USER_TOKEN_REFERENCE_NAME = cookiename;
        	} else {
        		USER_TOKEN_REFERENCE_NAME = "whydahusertoken_sso_" + ConstantValues.SSO_URI
        				.replace("https://", "")
        		.replace("http://", "")
        		.replace(":", "")
        		.replace("?", "")
        		.replace("&", "")
        		.replace("/", "_");
        	}
        	
           
        } catch (IOException e) {
            log.warn("AppConfig.readProperties failed. cookiedomain was set to {}", cookiedomain, e);
        }
    }

    public static void addSecurityHTTPHeaders(HttpServletResponse response) {
        //TODO Vi trenger en plan her.
        //response.setHeader("X-Frame-Options", "sameorigin");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "master-only");
    }


    public static void createAndSetUserTokenCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
        cookie.setValue(userTokenId);

        if (tokenRemainingLifetimeSeconds == null) {
            tokenRemainingLifetimeSeconds = DEFAULT_COOKIE_MAX_AGE;
        }
//        cookie.setMaxAge(tokenRemainingLifetimeSeconds);
//
//        if (cookiedomain != null && !cookiedomain.isEmpty()) {
//            cookie.setDomain(cookiedomain);
//        }
//        //cookie.setPath("/; secure; HttpOnly");
//        cookie.setPath("; HttpOnly;");
//        cookie.setSecure(IS_MY_URI_SECURED);
////        if ("https".equalsIgnoreCase(request.getScheme())) {
////            cookie.setSecure(true);
////        } else {
////            log.warn("Unsecure session detected, using myuri to define coocie security");
////            //cookie.setSecure(secureCookie(MY_APP_URI));
////            cookie.setSecure(false);
////
////        }
//
//        log.debug("Created cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
//                cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
//        response.addCookie(cookie);
        addCookie(cookie.getValue(), tokenRemainingLifetimeSeconds, response);
    }

    public static void updateUserTokenCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getUserTokenCookie(request);
        if(cookie==null){
        	cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
        }
        updateCookie(cookie, userTokenId, tokenRemainingLifetimeSeconds, response);
    }

    private static void updateCookie(Cookie cookie, String cookieValue, Integer tokenRemainingLifetimeSeconds, HttpServletResponse response) {
        if (cookieValue != null) {
            cookie.setValue(cookieValue);
        }
        //Only name and value are sent back to the server from the browser. The other attributes are only used by the browser to determine of the cookie should be sent or not.
        //http://en.wikipedia.org/wiki/HTTP_cookie#Setting_a_cookie
//
        if (tokenRemainingLifetimeSeconds == null) {
            tokenRemainingLifetimeSeconds = DEFAULT_COOKIE_MAX_AGE;
        }
//        cookie.setMaxAge(tokenRemainingLifetimeSeconds);
//
//        if (cookiedomain != null && !cookiedomain.isEmpty()) {
//            cookie.setDomain(cookiedomain);
//        }
//        cookie.setPath("; HttpOnly;");
//        cookie.setSecure(IS_MY_URI_SECURED);
//        log.debug("Created/updated cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
//                cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
//        response.addCookie(cookie);
        
        addCookie(cookie.getValue(), tokenRemainingLifetimeSeconds, response);
    }
    
    public static void clearUserTokenCookies(HttpServletRequest request, HttpServletResponse response) {
    	log.info("clear the cookie " + USER_TOKEN_REFERENCE_NAME);
//    	Cookie cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, "");
//    	cookie.setMaxAge(0);
//    	cookie.setPath("/");
//        response.addCookie(cookie);
        
        Cookie cookie = getUserTokenCookie(request);
        if (cookie != null) {
            if (cookiedomain != null && !cookiedomain.isEmpty()) {
                cookie.setDomain(cookiedomain);
            }
            addCookie(null, 0, response);
        }
        
    	//Cookie cookie = getUserTokenCookie(request);
        //if (cookie != null) {
//            cookie.setValue("");
//            cookie.setMaxAge(0);
//            if (cookiedomain != null && !cookiedomain.isEmpty()) {
//                cookie.setDomain(cookiedomain);
//            }
//            //cookie.setPath("/ ; HttpOnly;");
//            //cookie.setPath("/; secure; HttpOnly");
//            cookie.setPath("; HttpOnly;");
//            cookie.setSecure(IS_MY_URI_SECURED);
////            if ("https".equalsIgnoreCase(request.getScheme())) {
////                cookie.setSecure(true);
////            } else {
////                log.warn("Unsecure session detected, using myuri to define coocie security");
////                cookie.setSecure(secureCookie(MY_APP_URI));
////            }
//            response.addCookie(cookie);
//            log.trace("Cleared cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
//                    cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
            //addCookie("", 0, response);
            
            
        //}
    }

    public static String getUserTokenId(HttpServletRequest request) {
        String userTokenId = request.getParameter(CookieManager.USER_TOKEN_REFERENCE_NAME);
        if (userTokenId != null && userTokenId.length() > 1) {
            log.warn("getUserTokenId: userTokenIdFromRequest={}", userTokenId);
        } else {
            userTokenId = CookieManager.getUserTokenIdFromCookie(request);
            log.warn("getUserTokenId: userTokenIdFromCookie={}", userTokenId);
        }
        return userTokenId;
    }

    public static String getUserTokenIdFromCookie(HttpServletRequest request) {
        Cookie userTokenCookie = getUserTokenCookie(request);
        String userTokenId = null;
        if (userTokenCookie != null) {
            userTokenId = userTokenCookie.getValue();
        }
        return userTokenId;
    }

    private static Cookie getUserTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            log.debug("getUserTokenCookie: cookie with name={}, value={}", cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath());
            if (USER_TOKEN_REFERENCE_NAME.equalsIgnoreCase(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
    
    private static void addCookie(String userTokenId,
			Integer tokenRemainingLifetimeSeconds, HttpServletResponse response) {
//		StringBuilder sb = new StringBuilder(USER_TOKEN_REFERENCE_NAME);
//        sb.append("=");
//        sb.append(userTokenId);
//        sb.append(";expires=");
//        sb.append(tokenRemainingLifetimeSeconds);
//        sb.append(";path=");
//        sb.append("/");
//        sb.append(";HttpOnly");
//        if(IS_MY_URI_SECURED){
//        	 sb.append(";secure");
//        }
//        response.setHeader("SET-COOKIE", sb.toString());
        
        if (UserTokenId.isValid(userTokenId)) {
            StringBuilder sb = new StringBuilder(USER_TOKEN_REFERENCE_NAME);
            sb.append("=");
            sb.append(userTokenId==null?"":userTokenId);
            sb.append(";Max-Age=2147483647");
//        sb.append(";expires=");

            Date expdate = new Date();
            expdate.setTime(expdate.getTime() + (tokenRemainingLifetimeSeconds * 1000 * 60 * 24 * 14));
            DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz", Locale.ROOT);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
//        sb.append(df.format(expdate));
            sb.append(";path=");
            sb.append("/");
            sb.append(";domain=");
            sb.append(cookiedomain);
            sb.append(";HttpOnly");
            if (IS_MY_URI_SECURED) {
                sb.append(";secure");
            }
            response.setHeader("SET-COOKIE", sb.toString());
        }
	}
}
