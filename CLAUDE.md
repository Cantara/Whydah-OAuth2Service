# Whydah-OAuth2Service

## Purpose
OAuth2 and OpenID Connect provider-proxy for Whydah. Provides OAuth2/OIDC-compatible APIs and tokens by proxying to Whydah SecurityTokenService, enabling simplified integration with standard OAuth2 applications and frameworks.

## Tech Stack
- Language: Java 21
- Framework: Jersey 3.x, Jetty 12.x
- Build: Maven
- Key dependencies: Whydah-Admin-SDK, Hystrix, Jersey, Jetty

## Architecture
Standalone microservice that translates between OAuth2/OIDC protocols and Whydah's native token system. Maps OAuth2 client_id to Whydah applicationID, authorization_codes to applicationtokenids, and issues OAuth2 access_tokens backed by Whydah sessions. Supports authorization_code and client_credentials grant types.

## Key Entry Points
- `/authorize` - OAuth2 authorization endpoint
- `/token` - OAuth2 token endpoint
- `/verify` - Token verification endpoint
- `/health` - Health check

## Development
```bash
# Build
mvn clean install

# Run
java -jar target/Whydah-OAuth2Service-*.jar

# Verify
curl http://localhost:8086/Whydah-OAuth2Service/health
```

## Domain Context
OAuth2/OpenID Connect bridge for Whydah IAM. Enables standard OAuth2 integrations (third-party apps, SPAs, mobile apps) to authenticate through Whydah without custom integration code, using industry-standard OAuth2 flows.
