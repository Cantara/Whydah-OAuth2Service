package net.whydah.commands.application;


import net.whydah.commands.util.basecommands.BaseHttpGetHystrixCommand;

import java.net.URI;

public class CommandGetAllApplications extends BaseHttpGetHystrixCommand<String> {

    String uri;

    public CommandGetAllApplications(String uri){

        super(URI.create(uri), "hystrixGroupKey");
        this.uri = uri;
    }


    @Override
	protected String getTargetPath() {
        return "basicauthapplication/";
    }
}
