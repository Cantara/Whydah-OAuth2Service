package net.whydah.service.errorhandling;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AppExceptionMapper implements ExceptionMapper<AppException> {

	public Response toResponse(AppException ex) {
		
		return Response.status(ex.getStatus())
				.header("Cache-Control", "no-store")
        		.header("Pragma", "no-cache")
				.entity(ExceptionConfig.handleSecurity(new ErrorMessage(ex)).toString())
				.type(MediaType.APPLICATION_JSON).
				build();
	}

}
