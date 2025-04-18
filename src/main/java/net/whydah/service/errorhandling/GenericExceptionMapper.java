package net.whydah.service.errorhandling;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
 

	
	public Response toResponse(Throwable ex) {
		javax.ws.rs.WebApplicationException d;
		ErrorMessage errorMessage = new ErrorMessage();		
		setHttpStatus(ex, errorMessage);
		errorMessage.setCode(9999);
		errorMessage.setError(ex.getMessage());
		StringWriter errorStackTrace = new StringWriter();
		ex.printStackTrace(new PrintWriter(errorStackTrace));
		errorMessage.setErrorDescription(errorStackTrace.toString());
		errorMessage.setErrorUri("");
				
		return Response.status(errorMessage.getStatus())
				.header("Cache-Control", "no-store")
        		.header("Pragma", "no-cache")
				.entity(ExceptionConfig.handleSecurity(errorMessage).toString())
				.type(MediaType.APPLICATION_JSON)
				.build();	
	}

	private void setHttpStatus(Throwable ex, ErrorMessage errorMessage) {
		if (ex instanceof WebApplicationException exception) {
			errorMessage.setStatus(exception.getResponse().getStatus());
		} else {
			errorMessage.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); //defaults to internal server error 500
		}
	}
}