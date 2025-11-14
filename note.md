1. Spring cloud setup : config-repo, config-server, eureka-server,api-gateway
2. Authentication using JWT : auth-service, login signup functionality with access token and refresh token
3. FrontEnd : react application , login signup functionality in ui
4. Implement security at api-gatway: jwt token validation

keytool -genkeypair -alias jwt -keyalg RSA -keysize 2048 -keystore jwt.p12 -storetype PKCS12 -storepass  me@#23 -keypass me@#23 -dname "CN=auth-service, OU=Dev, O=insightvault, L=Pune, ST=Maharashtra, C=IN" -validity 3650

https://www.min.io/opensource