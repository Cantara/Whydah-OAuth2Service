package net.whydah.service.errorhandling;

import jakarta.ws.rs.core.Response.Status;




public class AppExceptionCode {
   
	public static final AppException AUTHORIZATIONCODE_NOTFOUND_8000 = new AppException(Status.BAD_REQUEST, 8000, "The authorization code is invalid or not found.", "The authorization code is invalid or not found.", "");
	public static final AppException USERTOKEN_INVALID_8001 = new AppException(Status.FORBIDDEN, 8001, "Invalid UserToken.", "Invalid UserToken.", "");
	public static final AppException CLIENT_NOTFOUND_8002 = new AppException(Status.BAD_REQUEST, 8002, "Client not found.", "Client not found.", "");
	public static final AppException SESSION_NOTFOUND_8003 = new AppException(Status.BAD_REQUEST, 8003, "Session not found.", "Session not found.", "");
	public static final AppException RESPONSETYPE_NOTSUPPORTED_8004 = new AppException(Status.BAD_REQUEST, 8004, "Response type not found.", "Response type not found.", "");
	public static final AppException REFRESHTOKEN_INVALID_8005 = new AppException(Status.BAD_REQUEST, 8005, "Invalid refresh token.", "Invalid refresh token.", "");
	public static final AppException SECRET_INVALID_8006 = new AppException(Status.BAD_REQUEST, 8006, "Invalid app serect.", "Invalid app serect.", "");
	public static final AppException APPLOGON_FAILED_8007 = new AppException(Status.BAD_REQUEST, 8007, "Applogon failed against STS.", "Applogon failed against STS. AppTokenXml=null from STS", "");
	public static final AppException CODECHALLENGEMETHOD_NOTSUPPORTED_8008 = new AppException(Status.BAD_REQUEST, 8008, "Transform algorithm not supported", "Transform algorithm not supported", "");
	public static final AppException CODEVERIFIER_INVALID_8009 = new AppException(Status.BAD_REQUEST, 8009, "Invalid code verifier", "Invalid code verifier", "");
	
	//MISC
	public static final AppException MISC_MISSING_PARAMS_9998 = new AppException(Status.BAD_REQUEST, 9998, "Missing required parameters", "Missing required parameters", "");
	public static final AppException MISC_BadRequestException_9997 = new AppException(Status.BAD_REQUEST, 9997, "BadRequestException", "", "");
	public static final AppException MISC_OperationFailedException_9996 = new AppException(Status.INTERNAL_SERVER_ERROR, 9996, "AuthenticationFailedException", "", "");
	public static final AppException MISC_ConflictException_9995 = new AppException(Status.INTERNAL_SERVER_ERROR, 9995, "ConflictException", "", "");
	public static final AppException MISC_RuntimeException_9994 = new AppException(Status.INTERNAL_SERVER_ERROR, 9994, "RuntimeException", "", "");
	public static final AppException MISC_FORBIDDEN_9993 = new AppException(Status.FORBIDDEN, 9993, "Forbidden", "", "");
	public static final AppException MISC_NotAuthorizedException_9992 = new AppException(Status.UNAUTHORIZED, 9992, "NotAuthorizedException", "", "");
	public static final AppException MISC_NOT_ACCEPTABLE_9991 = new AppException(Status.NOT_ACCEPTABLE, 9991, "Not Acceptable", "", "");
	public static final AppException MISC_NOTFOUND_9990 = new AppException(Status.NOT_FOUND, 9990, "Not found", "", "");
}
