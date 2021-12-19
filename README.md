# Whydah-OAuth2Service



![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/Cantara/Whydah-OAuth2Service) ![Build Status](https://jenkins.quadim.ai/buildStatus/icon?job=Whydah-OAuth2Service) ![GitHub commit activity](https://img.shields.io/github/commit-activity/m/Cantara/Whydah-OAuth2Service) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active)  [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/Whydah-OAuth2Service/badge.svg)](https://snyk.io/test/github/Cantara/Whydah-OAuth2Service)


The Whydah-OAuth2Service is an OAUTH2 and OpenID Connect provider-proxy for Whydah (STS) which supply oauth2 and OpenID Connect compatible APIs and tokens for 
easy simplified integration with OAUTH2 applications. The Whydah-OAuth2Service uses Whydah-SecurityTokenService as backend 
by mapping applicationtokenid's to OAUTH2 authorozation_codes by using the Whydah SDK Hystrix commands..

## How OAuth 2 Works

If you're just starting out with OAuth2, you might find these
resources useful:

- [OAuth 2 Simplified](https://www.digitalocean.com/community/tutorials/an-introduction-to-oauth-2)
- [An Introduction to OAuth 2](https://www.digitalocean.com/community/tutorials/an-introduction-to-oauth-2)


![The OAUTH2 flow](https://raw.githubusercontent.com/Cantara/Whydah-OAuth2Service/master/images/oauth2_flow.png)

This module in in early development state as we speak.

# Terminology mapping
| OAuth2 term | Whydah term | Comment |
| --- | --- | --- |
| client | application | |
| client_id | --applicationID-- | symmetric and padded mapping in module based on module configuration |
| client_secret | applicationSecret | |
| access_token | applicationToken or userToken |  |

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

# Configuration

![Whydah UserAdmin Configuration Example](https://raw.githubusercontent.com/Cantara/Whydah-OAuth2Service/master/images/whydah-oauth-module.png)


## OAUTH2 redirect configuration

![Whydah UserAdmin OAUTH2_REDIRECT Example](https://raw.githubusercontent.com/Cantara/Whydah-OAuth2Service/master/images/oauth2-redirect-registration-in-whydah.png)

## JWT Roles configuration

Whydah can optionally include user-roles as claims in the JWT. This is configured per scope using "App tags" of the 
application representing the relevant client.

In order to configure roles to be included as claims in the JWT for a given scope, define a tag starting with the prefix
`jwtroles-` and ending with the scope that the tag should apply to, e.g. `openid`. Note that the `jwtroles-` prefix 
defined in the tag-name is case-insensitive, and that scope is always treated as lowercase. The tag-value is a `;` 
(semicolon) separated list of user-role-name patterns that should be includes as claims in that user's JWT. The patterns
are used as either a literal (exact-match), or as a literal ending with a `*` wildcard (prefix-match).

### Example
For the scope `openid`, allow user-roles `foo`, `bar`, and all roles starting with `a`.

Application `MyApp` is configured with the following tag:
* tag-name: `JWTROLES-OPENID`
* tag-value: `foo;bar;a*`

User `me` is configured with the following roles:
* MyApp, foo, value-of-foo
* MyApp, bar, value-of-bar
* MyApp, zig, value-of-zig
* MyApp, abc, value-of-abc
* MyApp, ax, value-of-ax
* MyApp, bx, value-of-bx

The resulting JWT payload for the client of `MyApp` and user `me` would contain the following role claims. 
(Notice that roles `zig` and `bx` are not included):
```json
{
  ...
  "role_foo": "value-of-foo",
  "role_bar": "value-of-bar",
  "role_abc": "value-of-abc",
  "role_ax": "value-of-ax",
  ...
}
```
