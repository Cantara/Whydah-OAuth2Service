package no.cantara.commands.application;

import com.github.kevinsawicki.http.HttpRequest;
import no.cantara.commands.util.basecommands.BaseHttpPostHystrixCommand;

import java.net.URI;

public class CommandCreateApplication extends BaseHttpPostHystrixCommand<String> {
	
	private String json;
	
	public CommandCreateApplication(String json){

		super(URI.create("userAdminServiceUri"), "hystrixGroupKey");
		this.json = json;
	}
	
	@Override
	protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
		super.dealWithRequestBeforeSend(request);
        request.contentType("basicauthapplication/json").send(json);
        return request;
	}
	
	@Override
	protected String dealWithFailedResponse(String responseBody, int statusCode) {
		return statusCode + ":" + responseBody;
	}
	
	@Override
	protected String dealWithResponse(String response) {
		return "200" + ":" + super.dealWithResponse(response);
	}
	
	@Override
	protected String getTargetPath() {
        return "basicauthapplication/";
    }
}
