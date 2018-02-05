## Gradle Plugin

The Gradle plugin sources can be found in this ([kotlin-gradle-plugin](./)) module.

To install the Gradle plugin into the local Maven repository, run this command from the root of Kotlin project:

    ./gradlew :kotlin-gradle-plugin:install
    
The subplugin modules are `:kotlin-allopen`, `:kotlin-noarg`, `:kotlin-sam-with-receiver`. To install them, run:

    ./gradlew :kotlin-allopen:install :kotlin-noarg:install :kotlin-sam-with-receiver:install

To find more details about the plugins provided by this artifact and their tasks refer to [Module.md](Module.md).

### Gradle integration tests

Gradle integration tests can be found at the [kotlin-gradle-plugin-integration-tests](../kotlin-gradle-plugin-integration-tests) module.

Run the integration tests from the root project:

    ./gradlew :kotlin-gradle-plugin-integration-tests:test
    
The tests that use the Gradle plugins DSL ([`PluginsDslIT`](../kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/PluginsDslIT.kt)) also require the Gradle plugin marker artifacts to be installed:

    ./gradlew -Pdeploy_version=1.2-test :kotlin-gradle-plugin:plugin-marker:install :kotlin-noarg:plugin-marker:install :kotlin-allopen:plugin-marker:install
    ./gradlew -Pdeploy_version=1.2-test :kotlin-gradle-plugin-integration-tests:test
