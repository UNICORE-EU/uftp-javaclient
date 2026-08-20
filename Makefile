#
# Runs integration test of the .zip package
# expects an auth server on localhost:9000
#

uftp-client:
	@mvn -B package -DskipTests 

integration-test: uftp-client
	@unzip target/uftp-client*.zip && mv uftp-client-* uftp-client
	@uftp-client/bin/uftp --version
	@uftp-client/bin/uftp info -v -u demouser:test123 https://localhost:9000/rest/auth
	@uftp-client/bin/uftp ls -v -u demouser:test123 https://localhost:9000/rest/auth/TEST:
	@uftp-client/bin/uftp cp -v -u demouser:test123 pom.xml https://localhost:9000/rest/auth/TEST:
