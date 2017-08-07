# Whydah-oauth2-provider



Quick build and verify
* mvn clean install
* java -jar target/Whydah-oauth2-provider-0.1-SNAPSHOT.jar
* wget http://localhost:8086/Whydah-oauth2-provider/health
* wget "http://localhost:8086/Whydah-oauth2-provider/token?grant_type=client_credentials&grant_type=client_credentials&client_id=CLIENT_ID&client_secret=CLIENT_SECRET"
* curl -X POST "http://localhost:8086/Whydah-oauth2-provider/token?grant_type=client_credentials&grant_type=client_credentials&client_id=CLIENT_ID&client_secret=CLIENT_SECRET"
* curl -X POST "http://localhost:8086/Whydah-oauth2-provider/token?grant_type=authorization_code&code=mycode&redirect_uri=http://ocalhost:8086/microservice-baseline/oauth2&client_id=CLIENT_ID&client_secret=CLIENT_SECRET"


Simulator
* curl -i -H "Authorization: Bearer AsT5OjbzRn430zqMLgV3Ia" http://localhost:8086/Whydah-oauth2-provider/verify
