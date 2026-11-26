## Kotlin Maven Tools

You can work with the Maven modules of this Maven project in IDEA from the [root IDEA project](../../../ReadMe.md#working-in-idea). After importing you'll be able to explore Maven projects and run goals directly from IDEA with the instruments on the right sidebar.
Check out the [Working in IDEA](#working-in-idea) section for tips on how to properly configure IDEA for working with Maven modules.

### Building

Before building this Maven project you need to build and install the required artifacts built with Gradle to the local maven repository, by issuing the following command in the root project:

    ./gradlew install publish

> Note: on Windows type `gradlew.bat` without the leading `./`

This command assembles and puts the artifacts to the local maven repository to be used by the subsequent maven build. 
And it publishes artifacts to local dir `<repo-root>/build/repo` repository, that is necessary for running Maven Integration Tests.

See also [root ReadMe.md, section "Building"](../../../ReadMe.md#building).


Then you can build maven artifacts with Maven:

    ./mvnw install

If your maven build is failing with Out-Of-Memory errors, set JVM options for maven in `MAVEN_OPTS` environment variable like this:

    MAVEN_OPTS="-Xmx2G"

### Working in IDEA

You should be able to work with both Gradle and Maven modules in the same IDEA project. Follow these steps to properly set up the Maven integration:

1. Run `./gradlew install publish` to get things bootstrapped.
2. Open IDEA settings, Go to Build, Execution, Deployment | Build Tools | Maven.
3. Select "Use settings from .mvn/maven.config".
4. Check the "Override" checkbox next to "User settings file" and provide the path to `maven-settings.xml` from libraries/ directory.
5. Switch to Maven | Runner tab and Select Delegate IDE build/run actions to Maven.
6. Check "Skip Tests" for quick builds.
7. And make sure JDK 17 is selected as a JDK for Maven.
8. Switch to "Running Tests" tab and make sure options to pass (argLine, systemPropertyVariables, etc..) from maven-surefire-plugin are set.

* Q: Do I need to provide configure any JDKs in Environment variables?
* A: No, you don't have to. There is the CompositeJDKProvider that will try automatically to find JDKs in the system. 
If for some reason it fails, set the variables in the Environment variables section of the Build, Execution, Deployment | Build Tools | Maven | Runner menu.
Set the following variables:

```
Name: JDK_1_8, Value: path-to-jdk-1_8
Name: JDK_17_0, Value: path-to-jdk-17
```

If you're having issues with unresolved symbols in code, run this command and then "Reload All Maven Projects" in the Maven tool window (see KT-85580 for a detailed explanation):

```shell
find ~/.m2/repository/org/jetbrains/kotlin \
  -type f \
  -path "*/2.5.20-dev-XXXX/*" \
  \( -name "_remote.repositories" -o -name "*.lastUpdated" \) \
  -delete
```

Replace XXXX with the build number from kotlin.bootstrap.version in libraries/pom.xml, and replace "2.5.20" with the current version prefix.

### Working with Maven Integration Tests

You can navigate to `libraries/kotlin-maven-plugin-test` and work with Maven integration tests.
It is possible to trigger them in IDEA. It will run maven install goal and create dedicated 
run configuration for selected JUnit tests.

To run all tests from the CLI, you can use the command:

```
export JDK_1_8=path-to-jdk-1_8
export JDK_17_0=path-to-jdk-17
# export other JDK_XX_0 variables if needed

# now run tests like so
./mvnw -pl tools/kotlin-maven-plugin-test integration-test
```
