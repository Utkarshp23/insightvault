1. Spring cloud setup : config-repo, config-server, eureka-server,api-gateway
2. Authentication using JWT : auth-service, login signup functionality with access token and refresh token
3. FrontEnd : react application , login signup functionality in ui
4. Implement security at api-gatway: jwt token validation (nimbus,jwks,token signing)
5. Generating .p12 file for token signing and validation
    keytool -genkeypair -alias jwt -keyalg RSA -keysize 2048 -keystore jwt.p12 -storetype PKCS12 -storepass  me@#23 -keypass me@#23 -dname "CN=auth-service, OU=Dev, O=insightvault, L=Pune, ST=Maharashtra, C=IN" -validity 3650

6. Setting up Document service (entities, controllers, services)
7. Settign up MinIO on local machine using docker
        docker run -p 9000:9000 -p 9001:9001 `
            --name minio `
            -v U:\minio\data:/data `
            -e MINIO_ROOT_USER=minioadmin `
            -e MINIO_ROOT_PASSWORD=minioadmin `
            minio/minio server /data --console-address ":9001"

8. Configuring MinIo in document-service 