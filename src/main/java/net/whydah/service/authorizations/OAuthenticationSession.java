package net.whydah.service.authorizations;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public class OAuthenticationSession implements Serializable {
	
	
	private String id;
	private String scope;
	private String response_type;
	private String response_mode;
	private String client_id;
	private String redirect_uri;
	private String state;
	private String nonce;
	private String logged_in_users;
	private Date timeCreated;
	private String code_challenge;
	private String code_challenge_method;
	private String referer_channel;

	public OAuthenticationSession(String scope, String response_type, String response_mode, String client_id, String redirect_uri, String state, String nonce, String code_challenge, String code_challenge_method, String logged_in_users, String referer_channel, Date timeCreated) {
		this.id = UUID.randomUUID().toString();
		this.scope = scope;
		this.response_type = response_type;
		this.response_mode = response_mode;
		this.client_id = client_id;
		this.redirect_uri = redirect_uri;
		this.state = state;
		this.nonce = nonce;
		this.logged_in_users = logged_in_users;
		this.referer_channel = referer_channel;
		this.setCode_challenge(code_challenge);
		this.setCode_challenge_method(code_challenge_method);
		this.setTimeCreated(timeCreated);
	}

	public String getScope() {
		return scope;
	}
	public void setScope(String scope) {
		this.scope = scope;
	}
	public String getResponse_type() {
		return response_type;
	}
	public void setResponse_type(String response_type) {
		this.response_type = response_type;
	}
	public String getClient_id() {
		return client_id;
	}
	public void setClient_id(String client_id) {
		this.client_id = client_id;
	}
	public String getRedirect_uri() {
		return redirect_uri;
	}

	public void setRedirect_uri(String redirect_uri) {
		this.redirect_uri = redirect_uri;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getTimeCreated() {
		return timeCreated;
	}

	public void setTimeCreated(Date timeCreated) {
		this.timeCreated = timeCreated;
	}

	public String getLogged_in_users() {
		return logged_in_users;
	}

	public void setLogged_in_users(String logged_in_users) {
		this.logged_in_users = logged_in_users;
	}

	public String getResponse_mode() {
		if(response_mode!=null) {
			return response_mode;
		} else {
			if(response_type.equalsIgnoreCase("code") || response_type.equalsIgnoreCase("none")) {
				response_mode = "query";
			} else {
				response_mode = "fragment";
			}
			return response_mode;
		}	
	}

	public void setResponse_mode(String response_mode) {
		this.response_mode = response_mode;
	}

	public String getCode_challenge() {
		return code_challenge;
	}

	public void setCode_challenge(String code_challenge) {
		this.code_challenge = code_challenge;
	}

	public String getCode_challenge_method() {
		return code_challenge_method;
	}

	public void setCode_challenge_method(String code_challenge_method) {
		this.code_challenge_method = code_challenge_method;
	}

	public String getReferer_channel() {
		return referer_channel;
	}

	public void setReferer_channel(String referer_channel) {
		this.referer_channel = referer_channel;
	}

}
