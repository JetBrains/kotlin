## Kotlin Libraries

This area of the project is all written in Kotlin and assumes you've got the [Kotlin IDEA plugin installed](http://confluence.jetbrains.net/display/Kotlin/Getting+Started).

This area of the project uses Maven for its build. To build install a recent [Maven]() distribution then type:

    mvn install

For more details see the [Getting Started Guide](http://confluence.jetbrains.net/display/Kotlin/Getting+Started)

If you have a local Kotlin distribution (by running the Ant build in the parent directory) you can use your local Kotlin build via

    mvn install -PlocalKotlin
