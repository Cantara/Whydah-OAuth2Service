package net.whydah.service.oauth2proxyserver;

import javax.json.JsonObjectBuilder;

import io.jsonwebtoken.Claims;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.whydah.util.JwtUtils;

@Path(OAuth2UserResource.OAUTH2USERINFO_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2UserResource {
	public static final String OAUTH2USERINFO_PATH = "/userinfo";

	private final TokenService tokenService;


	
	@Inject
	public OAuth2UserResource(TokenService tokenService) {
		this.tokenService = tokenService;
	}


	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUserInfo(@Context HttpServletRequest request) throws Exception {

		Claims claims = JwtUtils.parseRSAJwtToken(request);
		if(claims == null) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
		JsonObjectBuilder tokenBuilder = tokenService.buildUserInfo(claims);
		if(tokenBuilder==null) {
			return Response.status(Response.Status.UNAUTHORIZED).build();	
		}
		return Response.ok(tokenBuilder.build().toString()).build();
	}
}
