## Gradle Plugin

The Gradle plugin sources can be found in this ([kotlin-gradle-plugin](./)) module.

To install the Gradle plugin into the local Maven repository, run this command from the root of Kotlin project:

    ./gradlew :kotlin-gradle-plugin:install
    
The subplugin modules are `:kotlin-allopen`, `:kotlin-noarg`, `:kotlin-sam-with-receiver`. To install them, run:

    ./gradlew :kotlin-allopen:install :kotlin-noarg:install :kotlin-sam-with-receiver:install

To find more details about the plugins provided by this artifact and their tasks refer to [Module.md](Module.md).

### Gradle Plugin Integration Tests

See the module [`libraries/tools/kotlin-gradle-plugin-integration-tests`](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-gradle-plugin-integration-tests)
