## Kotlin Libraries

This area of the project is all written in Kotlin and assumes you've got the [Kotlin IDEA plugin installed](http://confluence.jetbrains.net/display/Kotlin/Getting+Started).

This area of the project uses Maven for its build. You need to install a recent [Maven](http://maven.apache.org/) distribution and
setup environment variables as following:

    JDK_16="path to JDK 1.6"
    JDK_17="path to JDK 1.7"
    JDK_18="path to JDK 1.8"

The main part of kotlin runtime and all tools are compiled against JDK 1.6 and also there are two extensions
for the standard library which are compiled against JDK 1.7 and 1.8 respectively, so you need to have all these JDKs installed.

Then you'll be able to build tools and libraries with:

    mvn install

For more details see the [Getting Started Guide](http://confluence.jetbrains.net/display/Kotlin/Getting+Started).

Be sure to build Kotlin compiler distribution before launching Maven: see ReadMe.md at root level, section "Building".

If your maven build is failing with Out-Of-Memory errors, set JVM options for maven in MAVEN_OPTS environment variable like this:

    MAVEN_OPTS="-Xms512m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m"
