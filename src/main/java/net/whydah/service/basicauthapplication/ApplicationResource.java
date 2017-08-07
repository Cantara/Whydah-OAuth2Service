package net.whydah.service.basicauthapplication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-12.
 */
@Path(ApplicationResource.APPLICATION_PATH)
public class ApplicationResource {
    public static final String APPLICATION_PATH = "/application";
    private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String application="{}";
    private final String applications="{}";
    private final String applicationStatus="{}";

    @Autowired
    public ApplicationResource() {
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApplication(String json) {
        log.trace("Invoked createApplication with {}", json);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        String artifactId =  JsonPath.read(document, "$.artifactId");
        if (artifactId == null) {
            Response.Status status = Response.Status.BAD_REQUEST;
            log.warn("Invalid json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
            return Response.status(status).build();
        }


        String createdJson;
        try {
            createdJson = mapper.writeValueAsString(application);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", application.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(createdJson).build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllApplications() {
        log.trace("getAllApplications");
        String jsonResponse;
        try {
            jsonResponse = mapper.writeValueAsString(applications);
        } catch (JsonProcessingException e) {
            log.warn("Could not convert to Json {}", applications);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResponse).build();
    }

    @GET
    @Path("/{artifactId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusForArtifactInstances(@PathParam("artifactId") String artifactId) {
        log.trace("getStatusForArtifactInstances, artifactId={}", artifactId);


        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(applicationStatus);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", applicationStatus);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(jsonResult).build();
    }
}
