## Kotlin Libraries

This part of the project contains the sources of the following libraries:

  - [kotlin-stdlib](stdlib), the standard library for Kotlin/JVM, Kotlin/JS and its additional parts for JDK 7 and JDK 8
  - [kotlin-reflect](reflect), the library for full reflection support
  - [kotlin-test](kotlin.test), the library for multiplatform unit testing
  - [kotlin-annotations-jvm](tools/kotlin-annotations-jvm), the annotations to improve types in the Java code to look better when being consumed in the Kotlin code.

<!--  - [kotlin-annotations-android](tools/kotlin-annotations-android) -->

These libraries are built as a part of the [root](../) Gradle project.


## Kotlin Maven Tools

<!-- TODO: Move to another root -->

This area of the project is the root for Maven build.

You can work with the maven modules of this maven project in IDEA from the [root IDEA project](../ReadMe.md#working-in-idea). After importing you'll be able to explore maven projects and run goals directly from IDEA with the instruments on the right sidebar.

### Building

You need to install a recent (at least 3.3) [Maven](http://maven.apache.org/) distribution.

Before building this Maven project you need to build and install the required artifacts built with Gradle to the local maven repository, by issuing the following command in the root project:

    ./gradlew install

> Note: on Windows type `gradlew` without the leading `./`

This command assembles and puts the artifacts to the local maven repository to be used by the subsequent maven build.
See also [root ReadMe.md, section "Building"](../ReadMe.md#building).


Then you can build maven artifacts with Maven:

    mvn install

If your maven build is failing with Out-Of-Memory errors, set JVM options for maven in `MAVEN_OPTS` environment variable like this:

    MAVEN_OPTS="-Xmx2G"

