## Kotlin Libraries

This area of the project is all written in Kotlin and assumes you've got the [Kotlin IDEA plugin installed](../ReadMe.md#installing-plugin).

This area of the project uses Gradle and Maven for its build. When you open this project directory in IDEA the first time, it suggests you to import both gradle and maven projects. After importing you'll be able to explore and run gradle tasks and maven goals directly from IDEA with the instruments on the right sidebar.

### Building

You need to install a recent [Maven](http://maven.apache.org/) distribution and setup environment variables as following:

    JAVA_HOME="path to JDK 1.8"
    JDK_16="path to JDK 1.6"
    JDK_17="path to JDK 1.7"
    JDK_18="path to JDK 1.8"

The main part of the Kotlin standard library, `kotlin-stdlib`, is compiled against JDK 1.6 and also there are two extensions
for the standard library, `kotlin-stdlib-jre7` and `kotlin-stdlib-jre8`, which are compiled against JDK 1.7 and 1.8 respectively,
so you need to have all these JDKs installed.

Be sure to build Kotlin compiler distribution before launching Gradle and Maven: see [root ReadMe.md, section "Building"](../ReadMe.md#installing-plugin).

Core libraries are built with gradle, you can run that build using the gradle wrapper script even without local gradle installation:
    
    ./gradlew build install
    
> Note: on Windows type `gradlew` without the leading `./`
    
This command executes the `build` task, which assembles the artifacts and run the tests, and the `install` task, which puts the artifacts to the local maven repository to be used by the subsequent maven build.

The rest of tools and libraries are built with maven:

    mvn install

If your maven build is failing with Out-Of-Memory errors, set JVM options for maven in `MAVEN_OPTS` environment variable like this:

    MAVEN_OPTS="-Xmx2G"

## Gradle Plugin

Gradle plugin sources can be found at the [kotlin-gradle-plugin](tools/kotlin-gradle-plugin) module.

To build only gradle plugin and necessary dependencies use the following command:
```bash
mvn clean install -pl :kotlin-gradle-plugin -am
# to skip all tests also add -DskipTests -PnoTest
```

### Gradle integration tests

Gradle integration tests can be found at the [kotlin-gradle-plugin-integration-tests](tools/kotlin-gradle-plugin-integration-tests) module.

These tests are slow, so they are *skipped by default*.

To run integration tests use the `run-gradle-integration-tests` profile:
```bash
mvn clean install -pl :kotlin-gradle-plugin-integration-tests -am -Prun-gradle-integration-tests
```