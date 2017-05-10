## Test Server for vertx3-eventbus-rx-client
A Vert.x server implementation to run [vertx3-eventbus-rx-client](https://github.com/hcsalis/vertx3-eventbus-rx-client) end-to-end tests.

### Running the server
* Make sure java 8+ installed on your system
* Navigate into project dir.
* Build executable fat-jar `./gradlew shadowJar`
* Run fat-jar `java -jar ./build/libs/vertx3-eventbus-rx-client-test-server-fat.jar` 