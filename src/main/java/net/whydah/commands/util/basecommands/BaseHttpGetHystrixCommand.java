package net.whydah.commands.util.basecommands;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kevinsawicki.http.HttpRequest;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

import net.whydah.commands.util.HttpSender;
import net.whydah.commands.util.StringConv;

public abstract class BaseHttpGetHystrixCommand<R> extends HystrixCommand<R>{

	protected Logger log;
	protected URI serviceUri;
	protected String TAG="";
	protected HttpRequest request;
	
	protected BaseHttpGetHystrixCommand(URI serviceUri, String hystrixGroupKey, int hystrixExecutionTimeOut) {
		super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixGroupKey)).
				andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
						.withExecutionTimeoutInMilliseconds(hystrixExecutionTimeOut)));
		init(serviceUri, hystrixGroupKey);
	}

	protected BaseHttpGetHystrixCommand(URI serviceUri, String hystrixGroupKey) {
		super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixGroupKey)));
		init(serviceUri, hystrixGroupKey);
	}


	private void init(URI serviceUri, String hystrixGroupKey) {
		this.serviceUri = serviceUri;
		this.TAG =this.getClass().getSimpleName() + ", pool: " + hystrixGroupKey;
		this.log =  LoggerFactory.getLogger(TAG);
		HystrixRequestContext.initializeContext();
	}


	

	
	@Override
	protected R run() {
		return doGetCommand();

	}

	protected R doGetCommand() {
		try{
			String uriString = serviceUri.toString();
			if(getTargetPath()!=null){
				 uriString += getTargetPath();
			}

			log.debug("TAG" + " - serviceUri={}", uriString);
		
			
			
			if(getQueryParameters()!=null && getQueryParameters().length!=0){
				request = HttpRequest.get(uriString, true, getQueryParameters());
			} else {
				request = HttpRequest.get(uriString);
			}

			if(getAcceptHeaderRequestValue()!=null && !getAcceptHeaderRequestValue().equals("")){
				request = request.accept(getAcceptHeaderRequestValue());

            }
			
			request.trustAllCerts();
			request.trustAllHosts();
			
			if(getFormParameters()!=null && !getFormParameters().isEmpty()){
				request.contentType(HttpSender.APPLICATION_FORM_URLENCODED);
				request.form(getFormParameters());
			}

			request = dealWithRequestBeforeSend(request);
			
			
			responseBody = request.bytes();
			int statusCode = request.code();
			String responseAsText = StringConv.UTF8(responseBody);
			
			switch (statusCode) {
			case java.net.HttpURLConnection.HTTP_OK:
				onCompleted(responseAsText);
				return dealWithResponse(responseAsText);
			default:
				onFailed(responseAsText, statusCode);
				return dealWithFailedResponse(responseAsText, statusCode);
			}
		} catch(Exception ex){
			ex.printStackTrace();
			throw new RuntimeException("TAG" +  " - Application authentication failed to execute");
		}
	}

	protected R dealWithFailedResponse(String responseBody, int statusCode) {
		return null;
	}

	protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
		
		//CAN USE MULTIPART
		
		//JUST EXAMPLE
		
		//		HttpRequest request = HttpRequest.post("http://google.com");
		//		request.part("status[body]", "Making a multipart request");
		//		request.part("status[image]", new File("/home/kevin/Pictures/ide.png"));

		//OR SEND SOME DATA
		
		//HttpRequest.post("http://google.com").send("name=kevin")
		
		return request;
	}

	private void onFailed(String responseBody, int statusCode) {
		log.debug(TAG + " - Unexpected response from {}. Status code is {} content is {} ", serviceUri, String.valueOf(statusCode) + responseBody);
	}


	private void onCompleted(String responseBody) {
		log.debug(TAG + " - ok: " + responseBody);
	}


	protected abstract String getTargetPath();
	protected Map<String, String> getFormParameters(){
		return new HashMap<String, String>();
	}
	protected Object[] getQueryParameters(){
		return new String[]{};
	}

    protected Map<String, String> getHeaderParameters() {
        return new HashMap<String, String>();
    }


	@SuppressWarnings("unchecked")
	protected R dealWithResponse(String response){
		return (R)response;
	}

	@Override
	protected R getFallback() {
		log.warn(TAG + " - fallback - serviceUri={}", serviceUri.toString() + getTargetPath());
		// TODO - this should return false for Boolean basecommands
		return null;
	}


	protected String getAcceptHeaderRequestValue(){
		//CAN RETURN JSON (can be used in derived class)
		//return "cs_application/json";
		return "";
		
	}
	
	private byte[] responseBody;
	public byte[] getResponseBodyAsByteArray(){
		return responseBody;
	}
}
