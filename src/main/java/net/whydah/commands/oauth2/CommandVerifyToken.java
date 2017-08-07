package net.whydah.commands.oauth2;

import com.github.kevinsawicki.http.HttpRequest;
import net.whydah.commands.util.basecommands.BaseHttpGetHystrixCommand;

import java.net.URI;

public class CommandVerifyToken extends BaseHttpGetHystrixCommand<String> {

    String uri;
    String token;

    public CommandVerifyToken(String uri, String token) {

        super(URI.create(uri), "systemevents");
        this.uri = uri;
        this.token = token;
    }


    @Override
    protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
        return request.authorization("Bearer " + token);
    }

    @Override
    protected String getTargetPath() {
        return "/verify";
    }
}

