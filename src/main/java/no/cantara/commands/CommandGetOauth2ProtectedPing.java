package no.cantara.commands;

import com.github.kevinsawicki.http.HttpRequest;
import no.cantara.commands.config.ConstantValue;
import no.cantara.commands.util.basecommands.BaseHttpGetHystrixCommand;

import java.net.URI;

public class CommandGetOauth2ProtectedPing extends BaseHttpGetHystrixCommand<String> {

    String uri;

    public CommandGetOauth2ProtectedPing(String uri) {

        super(URI.create(uri), "hystrixGroupKey");
        this.uri = uri;
    }


    @Override
    protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
        return request.authorization("Bearer " + ConstantValue.ATOKEN);
    }

    @Override
    protected String getTargetPath() {
        return "/ping";
    }
}

