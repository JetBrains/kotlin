## Kotlin Libraries

This area of the project is all written in Kotlin and assumes you've got the [Kotlin IDEA plugin installed](http://confluence.jetbrains.net/display/Kotlin/Getting+Started).

This area of the project uses Maven for its build. To build install a recent [Maven](http://maven.apache.org/) distribution then type:

    mvn install

For more details see the [Getting Started Guide](http://confluence.jetbrains.net/display/Kotlin/Getting+Started)

Be sure to build Kotlin compiler before launching Maven: see ReadMe.md at root level, section "Building"

If your maven build is failing with Out-Of-Memory errors, set JVM options for maven in MAVEN_OPTS environment variable like this:

    MAVEN_OPTS="-Xms512m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m"
