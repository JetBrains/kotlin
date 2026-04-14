## Kotlin Libraries

This part of the project contains the sources of the following libraries:

  - [kotlin-stdlib](stdlib), the standard library for Kotlin/JVM, Kotlin/JS and its additional parts for JDK 7 and JDK 8
  - [kotlin-reflect](reflect), the library for full reflection support
  - [kotlin-test](kotlin.test), the library for multiplatform unit testing
  - [kotlin-annotations-jvm](tools/kotlin-annotations-jvm), the annotations to improve types in the Java code to look better when being consumed in the Kotlin code.

These libraries are built as a part of the [root](../) Gradle project.


## Kotlin Maven Tools

<!-- TODO: Move to another root -->

This area of the project is the root for Maven build.

You can work with the maven modules of this maven project in IDEA from the [root IDEA project](../ReadMe.md#working-in-idea). After importing you'll be able to explore maven projects and run goals directly from IDEA with the instruments on the right sidebar.

### Building

Before building this Maven project you need to build and install the required artifacts built with Gradle to the local maven repository, by issuing the following command in the root project:

    ./gradlew install publish

> Note: on Windows type `gradlew` without the leading `./`

This command assembles and puts the artifacts to the local maven repository to be used by the subsequent maven build. 
And it publishes artifacts to local dir `../build/repo` repository, that is necessary for running Maven Integration Tests.

See also [root ReadMe.md, section "Building"](../ReadMe.md#building).


Then you can build maven artifacts with Maven:

    ./mvnw install

If your maven build is failing with Out-Of-Memory errors, set JVM options for maven in `MAVEN_OPTS` environment variable like this:

    MAVEN_OPTS="-Xmx2G"

### Working with Maven Integration Tests

Make sure you opened this `libraries` dir as root project in IDEA. This way it will be interpreted as a Maven project.
Configure IDEA to delegate build to Maven. Follow:

0. Run `../gradlew install publish` to get things bootsrrapped. (note that gradlew script located in the parent directory)
1. Open IDEA settings, Go to Build, Execution, Deployment | Build Tools | Maven.
2. Select "Use settings from .mvn/maven.config".
2. Switch to Maven | Runner tab and Select Delegate IDE build/run actions to Maven.
3. Check "Skip Tests" for quick builds.
4. And make sure JDK 17 is selected as a JDK for Maven.
5. Switch to "Running Tests" tab and make sure options to pass (argLine, systemPropertyVariables, etc..) from maven-surefire-plugin are set.
6. Now project can be re-imported in IDEA.

Q: Do I need to provide configure any JDKs in Environment variables?
A: No, you don't have to. There is the CompositeJDKProvider that will try automatically to find JDKs in the system.

Known problem: if you see that IDEA can't resolve kotlin-dev version during sync.
You can temporarily fix it by overriding maven-settings.xml in IDEA project settings.
Set it to `maven-settings.xml` from libraries/ directory.

You can navigate to `libraries/kotlin-maven-plugin-test` and work with maven integration tests.
It is possible to trigger them in IDEA. It will run maven install goal and create dedicated 
run configuration for selected JUnit tests.

To run all tests, you can use CLI command:

```
export JDK_1_8=path-to-jdk-1_8
export JDK_17_0=path-to-jdk-17
# export other JDK_XX_0 variables if needed

# now run tests like so
./mvnw -pl tools/kotlin-maven-plugin-test integration-test
```
