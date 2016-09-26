## Kotlin Libraries

This area of the project is all written in Kotlin and assumes you've got the [Kotlin IDEA plugin installed](http://confluence.jetbrains.net/display/Kotlin/Getting+Started).

This area of the project uses Maven for its build. To build install a recent [Maven](http://maven.apache.org/) distribution then type:

    mvn install

For more details see the [Getting Started Guide](http://confluence.jetbrains.net/display/Kotlin/Getting+Started)

Be sure to build Kotlin compiler before launching Maven: see ReadMe.md at root level, section "Building"

If your maven build is failing with Out-Of-Memory errors, set JVM options for maven in MAVEN_OPTS environment variable like this:

    MAVEN_OPTS="-Xms512m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m"

## Gradle Plugin

Gradle plugin sources can be found at the [kotlin-gradle-plugin](tools/kotlin-gradle-plugin) module.

To build only gradle plugin and necessary dependencies use the following command:
```bash
mvn clean install -pl :kotlin-gradle-plugin -am
# to skip all tests also add -DskipTests
```

### Gradle integration tests

Gradle integration tests can be found at the [kotlin-gradle-plugin-integration-tests](tools/kotlin-gradle-plugin-integration-tests) module.

These tests are slow, so they are *skipped by default*.

To run integration tests use the 'run-gradle-integration-tests' profile:
```bash
mvn clean install -pl :kotlin-gradle-plugin-integration-tests -am -Prun-gradle-integration-tests
```