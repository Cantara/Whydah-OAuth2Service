package net.whydah.demoservice.oauth2ping;

import net.whydah.commands.config.ConstantValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.time.Instant;

@Path(PingResource.PING_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class PingResource {

    public static final String PING_PATH = "/ping";
    private static final Logger log = LoggerFactory.getLogger(PingResource.class);
    public static final String OAUTH_TOKEN_BEARER = "Bearer";

    @GET
    public Response pingRequest(@Context HttpHeaders headers) throws MalformedURLException {

        if (headers != null) {
            log.debug(getClass().getName() + ": headers=" + headers);
            String tokenid = headers.getHeaderString("Authorization").substring((headers.getHeaderString("Authorization").indexOf(OAUTH_TOKEN_BEARER) + "Bearer".length() + 1), headers.getHeaderString("Authorization").length()).trim();
            log.debug("Called with tokenid: {}", tokenid);
            if (!headers.getHeaderString("Authorization").equalsIgnoreCase("Bearer " + ConstantValue.ATOKEN)) {
                log.error("Illegal OAUTH token provided");
                return Response.status(Response.Status.FORBIDDEN).build();
            }


        }
        log.trace("pingRequest");
        String response = String.format("{ \"response\": \"PONG\",  \"now\":\"%s\"}", Instant.now());
        //String response = String.format("PONG, now=%s", Instant.now());
        return Response.ok(response).build();
    }

}
