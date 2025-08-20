package net.whydah.service.authorizations;

import static net.whydah.service.authorizations.UserAuthorizationResource.USER_PATH;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import net.whydah.commands.config.ConstantValues;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;
import net.whydah.service.oauth2proxyserver.OAuth2ProxyAuthorizeResource;
import net.whydah.sso.ddd.model.user.UserTokenId;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.CookieManager;
import net.whydah.util.FreeMarkerHelper;
import net.whydah.util.UriBuilder;

/**
 * Created by baardl on 10.08.17.
 */
@Path(USER_PATH)
public class UserAuthorizationResource {
	private static final Logger log = getLogger(UserAuthorizationResource.class);
	public static final String USER_PATH = "/user";
	

	private final ClientService clientService;
	private final UserAuthorizationService userAuthorizationService;

	@Inject
	public UserAuthorizationResource(UserAuthorizationService userAuthorizationService, ClientService clientService) {
		this.userAuthorizationService = userAuthorizationService;
		this.clientService = clientService;
		
	}

    /**
     * https://<host>/<context_root>/user?client_id=testclient&scopes=scopes with space delimiter
     * eg: http://localhost:8086/Whydah-OAuth2Service/user?client_id=testclient&scopes=email%20nick
     * @param clientId
     * @param scope
     * @param request
     * @return
     * @throws AppException 
     * @throws UnsupportedEncodingException 
     */
    @GET
    public Response authorizationGui(
    		@QueryParam("oauth_session") String oauth_session, 
    		@QueryParam("userticket") String userticket, 
    		@QueryParam("cancelled") boolean cancelled,
    		@QueryParam("referer_channel") String referer_channel,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) throws AppException, UnsupportedEncodingException {
      
    	if(!cancelled) {
    		log.info("session establised in SSO and returned oauth_session {} - userticket {} - referer_channel {}", oauth_session, userticket, referer_channel);
    	}
    	OAuthenticationSession session = userAuthorizationService.getSSOSession(oauth_session);
    	if(session==null) {
    		throw AppExceptionCode.SESSION_NOTFOUND_8003;
    	} else {
    		Client client = clientService.getClient(session.getClient_id());
    		if(client==null) {
    			throw AppExceptionCode.CLIENT_NOTFOUND_8002;
    		} else {
    			
    			if(cancelled) {
    				return Response.seeOther(URI.create(session.getRedirect_uri())).build();
    			}
    			//solve usertoken
    			UserToken usertoken = null;
    			if(userticket!=null) {
    				usertoken = userAuthorizationService.findUserTokenFromUserTicket(userticket);
    			} else {
    				String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
    				if(userTokenIdFromCookie!=null && UserTokenId.isValid(userTokenIdFromCookie)) {
    					usertoken = userAuthorizationService.findUserTokenFromUserTokenId(userTokenIdFromCookie);
    					if(usertoken==null) {
    						CookieManager.clearUserTokenCookies(request, response);
    					}
    				}
    			}
    			
    			if(referer_channel!=null) {
    				//override from SSO
    				session.setReferer_channel(referer_channel);
    			}
    			
    			if (usertoken == null) {
					return userAuthorizationService.toSSO(session.getClient_id(), session.getScope(), session.getResponse_type(), session.getResponse_mode(), session.getState(), session.getNonce(), session.getRedirect_uri(), session.getLogged_in_users(), session.getReferer_channel(), session.getCode_challenge(), session.getCode_challenge_method());
				} else {
					boolean suppress_consent = session.getLogged_in_users().contains(usertoken.getUserName());
					if(suppress_consent || !ConstantValues.CONSENT_SCOPES_ENABLED) {
						
						String directUri = UriBuilder
								.fromUriString(ConstantValues.MYURI.replaceFirst("/$", "") +"/" + OAuth2ProxyAuthorizeResource.OAUTH2AUTHORIZE_PATH + "/acceptance" )
								.queryParam("client_id", session.getClient_id())
								.queryParam("redirect_uri", URLEncoder.encode(clientService.getRedirectURI(session.getClient_id(), session.getRedirect_uri()), "utf-8"))
								.queryParam("response_type", URLEncoder.encode(session.getResponse_type(), "utf-8"))
								.queryParam("response_mode", session.getResponse_mode())
								.queryParam("scope", URLEncoder.encode(session.getScope(), "utf-8"))
								.queryParam("state", session.getState())
								.queryParam("nonce", session.getNonce())
								.queryParam("usertoken_id", usertoken.getUserTokenId())
								.queryParam("logged_in_users", URLEncoder.encode(session.getLogged_in_users(), "utf-8"))
								.queryParam("referer_channel", session.getReferer_channel())
								.queryParam("code_challenge", session.getCode_challenge())
								.queryParam("code_challenge_method", session.getCode_challenge_method())
								.toUriString();
						
						return Response.seeOther(URI.create(directUri)).build();
						
					} else {
						Map<String, Object> model = userAuthorizationService.buildUserModel(session.getClient_id(), client.getApplicationName(), session.getScope(), session.getResponse_type(), session.getResponse_mode(), session.getState(), session.getNonce(), session.getRedirect_uri(), usertoken.getUserTokenId(), session.getReferer_channel(), session.getCode_challenge(), session.getCode_challenge_method());
						model.put("logoURL", ConstantValues.getLogoUrl());

						String body = FreeMarkerHelper.createBody("/UserAuthorization.ftl", model);
						return Response.ok(body).build();

//						Viewable userAuthorizationGui = new Viewable("/UserAuthorization.ftl", model);
//						return Response.ok(userAuthorizationGui).build();
					}
					
				}

			}
		}
	}
//
//	public String createBody(String templateName, Map<String, Object> model) {
//		StringWriter stringWriter = new StringWriter();
//		try {
//			Template template = freemarkerConfig.getTemplate(templateName);
//			template.process(model, stringWriter);
//		} catch (RuntimeException e) {
//			throw e;
//		} catch (Exception e) {
//			throw new RuntimeException("Populating template failed. templateName=" + templateName, e);
//		}
//		return stringWriter.toString();
//	}
//
//
//	private void loadTemplates() {
//		try {
//			freemarkerConfig = new Configuration(Configuration.VERSION_2_3_0);
//			File customTemplate = new File("./templates");
//			FileTemplateLoader ftl = null;
//			if (customTemplate.exists()) {
//				ftl = new FileTemplateLoader(customTemplate);
//			}
//			ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/templates");
//
//			TemplateLoader[] loaders = null;
//			if (ftl != null) {
//				loaders = new TemplateLoader[]{ftl, ctl};
//			} else {
//				loaders = new TemplateLoader[]{ctl};
//			}
//
//			MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
//			freemarkerConfig.setTemplateLoader(mtl);
//			freemarkerConfig.setObjectWrapper(new DefaultObjectWrapper());
//			freemarkerConfig.setDefaultEncoding("UTF-8");
//			freemarkerConfig.setLocalizedLookup(false);
//			freemarkerConfig.setTemplateUpdateDelayMilliseconds(6000);
//		} catch (IOException ioe) {
//			log.error("Unable to load/process freemarker tenmplates", ioe);
//		}
//	}



}
