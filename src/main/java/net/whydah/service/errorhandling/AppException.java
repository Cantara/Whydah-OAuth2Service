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
		// Add comprehensive null checking
		this.status = status != null ? status : Status.INTERNAL_SERVER_ERROR;
		this.code = code;
		this.setError(message != null ? message : "Unknown error");
		this.error_description = developerMessage != null ? developerMessage : "";
		this.error_uri = link != null ? link : "";
	}

	public AppException() {
		this.status = Status.INTERNAL_SERVER_ERROR;
		this.code = 0;
		this.error = "Empty";
		this.error_description = "";
		this.error_uri = "";
	}

	public Status getStatus() {
		return status;
	}

	public AppException setStatus(Status status) {
		this.status = status != null ? status : Status.INTERNAL_SERVER_ERROR;
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
		this.error_description = developerMessage != null ? developerMessage : "";
		return this;
	}

	public AppException setErrorDescription(String developerMessage, Object...args) {
		if (developerMessage != null && args != null) {
			try {
				this.error_description = developerMessage.formatted(args);
			} catch (Exception e) {
				this.error_description = developerMessage;
			}
		} else {
			this.error_description = developerMessage != null ? developerMessage : "";
		}
		return this;
	}

	public AppException setErrorDescriptionParams(Object...args) {
		if (error_description != null && args != null) {
			try {
				this.error_description = error_description.formatted(args);
			} catch (Exception e) {
				// Keep original if formatting fails
			}
		}
		return this;
	}

	public String getErrorUri() {
		return error_uri;
	}

	public AppException setErrorUri(String link) {
		this.error_uri = link != null ? link : "";
		return this;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error != null ? error : "Empty";
	}
}