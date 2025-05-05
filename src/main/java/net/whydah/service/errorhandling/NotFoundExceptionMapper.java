package net.whydah.service.errorhandling;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

	public Response toResponse(NotFoundException ex) {
		return Response.status(ex.getResponse().getStatus())
				.header("Cache-Control", "no-store")
        		.header("Pragma", "no-cache")
				.entity(ExceptionConfig.handleSecurity(new ErrorMessage(ex)).toString())
				.type(MediaType.APPLICATION_JSON) //this has to be set to get the generated JSON 
				.build();
	}

}