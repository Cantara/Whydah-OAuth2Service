package net.whydah.service.oauth2proxyserver;

import org.glassfish.hk2.api.Immediate;

import jakarta.inject.Singleton;
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
@Singleton
@Immediate
public class OAuth2ProxyVerifyResource {
    public static final String OAUTH2TOKENVERIFY_PATH = "/verify";

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        // Replace StringUtils.hasText() with standard Java null/empty checks
        if (headerAuth != null && !headerAuth.trim().isEmpty() && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
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
}