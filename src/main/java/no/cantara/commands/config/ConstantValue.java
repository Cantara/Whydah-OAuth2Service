package no.cantara.commands.config;

import no.cantara.util.Configuration;

public class ConstantValue {

    public static final int COMMAND_TIMEOUT = 10000;

    public static final String ATOKEN = Configuration.getString("oauth.dummy.atoken");  //"AsT5OjbzRn430zqMLgV3Ia";
    public static final String UTOKEN = Configuration.getString("oauth.dummy.utoken");  //"usT5OjbzRn430zqMLgV3Ia";

}
