package net.whydah.commands.oauth2;

import com.github.kevinsawicki.http.HttpRequest;
import net.whydah.commands.util.basecommands.BaseHttpPostHystrixCommand;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CommandGetOAuth2AccessToken extends BaseHttpPostHystrixCommand<String> {


    private String code;
    private String redirectURI= "REDIRECT_URI";
    private String CLIENT_ID="CLIENT_ID";
    private String CLIENT_SECRET="CLIENT_SECRET";
    int retryCnt = 0;


    public CommandGetOAuth2AccessToken(String uri, String code, String redirectURI) {
        super(URI.create(uri), "hystrixGroupKey");

        this.code = code;
        this.redirectURI=redirectURI;
    }

    @Override
    protected String dealWithFailedResponse(String responseBody, int statusCode) {
        if (statusCode != HttpURLConnection.HTTP_CONFLICT && retryCnt < 1) {
            retryCnt++;
            return doPostCommand();
        } else {
            return null;
        }
    }


    @Override
    protected String getTargetPath() {
        return "/token"; // + "?grant_type=authorization_code" + "&code=" + code + "&redirect_uri=" + redirectURI +
//                "&client_id=" +CLIENT_ID + "&client_secret=" + CLIENT_SECRET;
    }

    @Override
    protected Map<String, String> getFormParameters() {
        Map<String,String> formParams = new HashMap<>();
        formParams.put("grant_type","authorization_code");
        formParams.put("client_id",CLIENT_ID);
        formParams.put("client_secret",CLIENT_SECRET);

        if (code != null) {
            formParams.put("code", code);
        }
        if (redirectURI != null) {
            formParams.put("redirect_uri", redirectURI);
        }

        return formParams;

    }



    @Override
    protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
        super.dealWithRequestBeforeSend(request);
        String authString = CLIENT_ID + ":" + CLIENT_SECRET;
        byte[] encoding = Base64.getEncoder().encode(authString.getBytes());
        return request.authorization("Basic " +  encoding);
    }


}
