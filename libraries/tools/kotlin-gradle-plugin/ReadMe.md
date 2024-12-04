## Gradle Plugin

The Gradle plugin sources can be found in this ([kotlin-gradle-plugin](./)) module.

To install the Gradle plugin into the local Maven repository, run this command from the root of Kotlin project:

    ./gradlew :kotlin-gradle-plugin:install
    
The subplugin modules are `:kotlin-allopen`, `:kotlin-noarg`, `:kotlin-sam-with-receiver`. To install them, run:

    ./gradlew :kotlin-allopen:install :kotlin-noarg:install :kotlin-sam-with-receiver:install

### Gradle Plugin Integration Tests

All Gradle plugins tests are located inside [`libraries/tools/kotlin-gradle-plugin-integration-tests`](../kotlin-gradle-plugin-integration-tests/Readme.md)
module.
