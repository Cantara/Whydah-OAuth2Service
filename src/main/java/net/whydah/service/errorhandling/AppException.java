package net.whydah.service.errorhandling;

import jakarta.ws.rs.core.Response.Status;

import java.io.Serial;


public class AppException extends Throwable {

    @Serial
	private static final long serialVersionUID = -8999932578270387947L;
	
	Status status;
	
	int code; 
		
	String error_uri;
	
	String error_description;	
	
	private String error;
	
	public AppException(Status status, int code, String message,
			String developerMessage, String link) {
		this.setError(message);
		this.status = status;
		this.code = code;
		this.error_description = developerMessage;
		this.error_uri = link;
	}

	public AppException() { }

	public Status getStatus() {
		return status;
	}

	public AppException setStatus(Status status) {
		this.status = status;
		return this;
	}

	public int getCode() {
		return code;
	}

	public AppException setCode(int code) {
		this.code = code;
		return this;
	}

	public String getErrorDescription() {
		return error_description;
	}

	public AppException setErrorDescription(String developerMessage) {
		this.error_description = developerMessage;
		return this;
	}
	public AppException setErrorDescription(String developerMessage, Object...args) {
        this.error_description = developerMessage.formatted(args);
		return this;
	}
	
	public AppException setErrorDescriptionParams(Object...args) {
        this.error_description = error_description.formatted(args);
		return this;
	}

	public String getErrorUri() {
		return error_uri;
	}

	public AppException setErrorUri(String link) {
		this.error_uri = link;
		return this;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
	
}