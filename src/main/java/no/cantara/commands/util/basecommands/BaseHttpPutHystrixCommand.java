package no.cantara.commands.util.basecommands;

import com.github.kevinsawicki.http.HttpRequest;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import no.cantara.commands.util.HttpSender;
import no.cantara.commands.util.StringConv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseHttpPutHystrixCommand<R> extends HystrixCommand<R> {

    protected Logger log;
    protected URI serviceUri;
    protected String TAG = "";
    protected HttpRequest request;
    
    protected BaseHttpPutHystrixCommand(URI serviceUri, String hystrixGroupKey, int hystrixExecutionTimeOut) {
		super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixGroupKey)).
				andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
						.withExecutionTimeoutInMilliseconds(hystrixExecutionTimeOut)));
		init(serviceUri, hystrixGroupKey);
	}

	protected BaseHttpPutHystrixCommand(URI serviceUri, String hystrixGroupKey) {
		super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixGroupKey)));
		init(serviceUri, hystrixGroupKey);
	}
	

    private void init(URI serviceUri,  String hystrixGroupKey) {
        this.serviceUri = serviceUri;
       
        this.TAG = this.getClass().getName() + ", pool :" + hystrixGroupKey;
        this.log = LoggerFactory.getLogger(TAG);
        HystrixRequestContext.initializeContext();
    }


    @Override
    protected R run() {
        return doPutCommand();

    }

    protected R doPutCommand() {
        try {
            String uriString = serviceUri.toString();
            if (getTargetPath() != null) {
                uriString += getTargetPath();
            }

            log.debug("TAG" + " - serviceUri={}", uriString);

            if (getQueryParameters() != null && getQueryParameters().length != 0) {
                request = HttpRequest.put(uriString, true, getQueryParameters());
            } else {
                request = HttpRequest.put(uriString);
            }
            request.trustAllCerts();
            request.trustAllHosts();

            if (getFormParameters() != null && !getFormParameters().isEmpty()) {
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
			
			
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("TAG" + " - Application authentication failed to execute");
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

        //request.send("name=huydo")
        //or something like
        //request.contentType("cs_application/json").send(applicationJson);

        return request;
    }

    protected void onFailed(String responseBody, int statusCode) {
        log.debug(TAG + " - Unexpected response from {}. Status code is {} content is {} ", serviceUri, String.valueOf(statusCode) + responseBody);
    }

    protected void onCompleted(String responseBody) {
        log.debug(TAG + " - ok: " + responseBody);
    }

    protected abstract String getTargetPath();

    protected Map<String, String> getFormParameters() {
        return new HashMap<String, String>();
    }

    protected Object[] getQueryParameters() {
        return new String[]{};
    }

    @SuppressWarnings("unchecked")
    protected R dealWithResponse(String response) {
        return (R) response;
    }

    @Override
    protected R getFallback() {
        log.warn(TAG + " - fallback - ServiceUri={}", serviceUri.toString() + getTargetPath());
        return null;
    }
    private byte[] responseBody;
	public byte[] getResponseBodyAsByteArray(){
		return responseBody;
	}
}

