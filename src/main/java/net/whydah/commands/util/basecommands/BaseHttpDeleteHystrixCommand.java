package net.whydah.commands.util.basecommands;

import com.github.kevinsawicki.http.HttpRequest;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import net.whydah.commands.util.StringConv;
import net.whydah.commands.util.HttpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseHttpDeleteHystrixCommand<R> extends HystrixCommand<R> {

    protected Logger log;
    protected URI serviceUri;
    protected String TAG = "";
    protected HttpRequest request;
    private byte[] responseBody;

    protected BaseHttpDeleteHystrixCommand(URI serviceUri, String hystrixGroupKey, int hystrixExecutionTimeOut) {
        super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixGroupKey)).
                andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionTimeoutInMilliseconds(hystrixExecutionTimeOut)));
        init(serviceUri, hystrixGroupKey);
    }


    protected BaseHttpDeleteHystrixCommand(URI serviceUri, String hystrixGroupKey) {
        super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixGroupKey)));
        init(serviceUri, hystrixGroupKey);
    }

    private void init(URI serviceUri, String hystrixGroupKey) {
        this.serviceUri = serviceUri;
        this.TAG = this.getClass().getName() + ", pool :" + hystrixGroupKey;
        this.log = LoggerFactory.getLogger(TAG);
        HystrixRequestContext.initializeContext();
    }

    @Override
    protected R run() {
        return doDeleteCommand();

    }

    protected R doDeleteCommand() {
        try {
            String uriString = serviceUri.toString();
            if (getTargetPath() != null) {
                uriString += getTargetPath();
            }

            log.debug("TAG" + " - serviceUri={}", uriString);

            if (getQueryParameters() != null && getQueryParameters().length != 0) {
                request = HttpRequest.delete(uriString, true, getQueryParameters());
            } else {
                request = HttpRequest.delete(uriString);
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
        log.warn(TAG + " - fallback {}", serviceUri.toString() + getTargetPath());
        return null;
    }

    public byte[] getResponseBodyAsByteArray() {
        return responseBody;
    }
}

