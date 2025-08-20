package net.whydah.service.errorhandling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ErrorMessage {
	
	/** contains the same HTTP Status code returned by the server */
	@XmlElement(name = "status")
	int status;
	
	/** application specific error code */
	@XmlElement(name = "code")
	int code;
	
	/** message describing the error*/
	@XmlElement(name = "error")
	String error;
		
	/** link point to page where the error message is documented */
	@XmlElement(name = "error_uri")
	String error_uri;
	
	/** extra information that might useful for developers */
	@XmlElement(name = "error_description")
	String error_description;	
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getError() {
		return error;
	}

	public void setError(String message) {
		this.error = message;
	}

	public String getErrorDescription() {
		return error_description;
	}

	public void setErrorDescription(String developerMessage) {
		this.error_description = developerMessage;
	}

	public String getErrorUri() {
		return error_uri;
	}

	public void setErrorUri(String link) {
		this.error_uri = link;
	}
	
	public ErrorMessage(AppException ex) {
		// Replace BeanUtils.copyProperties with manual property copying
		this.status = ex.getStatus().getStatusCode();
		this.code = ex.getCode();
		this.error = ex.getError();
		this.error_uri = ex.getErrorUri();
		this.error_description = ex.getErrorDescription();
	}
	
	public ErrorMessage(NotFoundException ex) {
		this.status = Response.Status.NOT_FOUND.getStatusCode();
		this.error = ex.getMessage();
		this.error_uri = "https://jersey.java.net/apidocs/2.8/jersey/javax/ws/rs/NotFoundException.html";		
	}
			
	public ErrorMessage() {}
	
	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}
}