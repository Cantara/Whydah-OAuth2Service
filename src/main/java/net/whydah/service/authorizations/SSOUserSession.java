package net.whydah.service.authorizations;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public class SSOUserSession implements Serializable {
	private String id;
	private String scope;
	private String response_type;
	private String client_id;
	private String redirect_uri;
	private String state;
	private String nonce;
	private String logged_in_users="";
	private Date timeCreated;

	public SSOUserSession(String scope, String response_type, String client_id, String redirect_uri, String state, String nonce, String logged_in_users, Date timeCreated) {
		this.id = UUID.randomUUID().toString();
		this.scope = scope;
		this.response_type = response_type;
		this.client_id = client_id;
		this.redirect_uri = redirect_uri;
		this.state = state;
		this.nonce = nonce;
		this.logged_in_users = logged_in_users;
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

}
