package net.whydah.service.oauth2proxyserver;

import org.glassfish.hk2.api.Immediate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.whydah.util.JwtUtils;

@Path(OAuth2ProxyVerifyResource.OAUTH2TOKENVERIFY_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Component
@Immediate
public class OAuth2ProxyVerifyResource {
	public static final String OAUTH2TOKENVERIFY_PATH = "/verify";


	private String parseJwt(HttpServletRequest request) {
		String headerAuth = request.getHeader("Authorization");
		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			return headerAuth.substring(7, headerAuth.length());
		}
		return null;
	}

	@GET
	public Response verify(@Context HttpServletRequest request) throws Exception {
		String jwt = parseJwt(request);
		if (jwt != null && JwtUtils.validateRSAJwtToken(jwt)) {
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
	}



	/*
	 * 
	 * OLD
	 * 
	 * 
	 * 
    private static final ObjectMapper mapper = new ObjectMapper();


    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyTokenResource.class);

    private final CredentialStore credentialStore;


    @Autowired
    public OAuth2ProxyVerifyResource(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    @GET
    public Response getOAuth2ProxyServerController(@Context HttpHeaders headers) throws MalformedURLException {

        if (headers != null) {
            log.debug(getClass().getName() + ": headers=" + headers);
            String autorizationHeader = headers.getHeaderString("Authorization");
            boolean hasValidAuth = validateAuthorization(autorizationHeader);
//            if (!headers.getHeaderString("Authorization").equalsIgnoreCase("Bearer " + ConstantValue.ATOKEN)) {
              if (!hasValidAuth) {
                  log.error("Illegal OAUTH token provided");http://localhost:8888/login
                  return Response.status(Response.Status.FORBIDDEN).build();
            }
        }
        if (credentialStore.hasWhydahConnection()){
            log.trace("getOAuth2ProxyServerController - check STS");
            // TODO - Call the STS
        }
        log.warn("getOAuth2ProxyServerController - no Whydah - dummy standalone fallback");

        String jsonResult = "";
        Map systemEventsJson = Configuration.getMap("oauth.dummy.verifiedtoken");
        try {
            jsonResult = mapper.writeValueAsString(systemEventsJson);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", jsonResult);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        log.trace("getOAuth2ProxyServerController - returning:{}", jsonResult);
        return Response.status(Response.Status.OK).entity(jsonResult).build();
    }

    private boolean validateAuthorization(String autorizationHeader) {
        boolean isValid = false;
        if (!isEmpty(autorizationHeader)) {
            //TODO clientStore or STS validate authToken
            isValid = true;
        }
        return isValid;
    }*/


}

