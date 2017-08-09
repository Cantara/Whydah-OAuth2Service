# Whydah-OAuth2Service

The Whydah-OAuth2Service is an OAUTH2 provider-proxy for Whydah (STS) which supply oauth2 compatible APIs and tokens for 
easy simplified integration with OAUTH2 applications. The Whydah-OAuth2Service uses Whydah-SecurityTokenService as backend 
by mapping applicationtokenid's to OAUTH2 authorozation_codes by using the Whydah SDK Hystrix commands..

![The OAUTH2 flow](https://raw.githubusercontent.com/Cantara/Whydah-OAuth2Service/master/images/oauth2_flow.png)

This module in in early development state as we speak.

# Terminology mapping
| OAuth2 term | Whydah term | Comment |
| --- | --- | --- |
| client | application | |
| client_id | applicationID | symmetric and padded mapping in module based on module configuration |
| client_secret | applicationSecret | |

Quick build and verify
* mvn clean install
* java -jar target/Whydah-OAuth2Service-0.4-SNAPSHOT.jar
* wget http://localhost:8086/Whydah-OAuth2Service/health
* curl -v http://localhost:8086/Whydah-OAuth2Service/authorize?response_type=code&client_id=CLIENT_ID
* wget "http://localhost:8086/Whydah-OAuth2Service/token?grant_type=client_credentials&grant_type=client_credentials&client_id=CLIENT_ID&client_secret=CLIENT_SECRET"
* curl -X POST "http://localhost:8086/Whydah-OAuth2Service/token?grant_type=client_credentials&grant_type=client_credentials&client_id=CLIENT_ID&client_secret=CLIENT_SECRET"
* curl -X POST "http://localhost:8086/Whydah-OAuth2Service/token?grant_type=authorization_code&code=mycode&redirect_uri=http://ocalhost:8086/Whydah-OAuth2Service/oauth2&client_id=CLIENT_ID&client_secret=CLIENT_SECRET"

Verify token
* curl -i -H "Authorization: Bearer AsT5OjbzRn430zqMLgV3Ia" http://localhost:8086/Whydah-OAuth2Service/verify

Access OAuth2 protected demo service
* curl -i -H "Authorization: Bearer AsT5OjbzRn430zqMLgV3Ia" http://localhost:8086/Whydah-OAuth2Service/ping
