# Kotlin serialization IDEA plugin

Kotlin serialization plugin consists of three parts: a compiler plugin, an IntelliJ plugin and a runtime library.
This is the IDEA plugin. Gradle and Maven plugins can be found in `libraries` folder.

Please note that this plugin currently works only for highlighting and resolving symbols, and it doesn't work with embedded IDEA compiler.
To build any project with serialization within IDEA, you'll need to delegate all build actions to gradle:
`File - Settings - Build, Execution, Deployment - Build Tools - Gradle - Runner -` tick `Delegate IDE build/run actions to gradle`.
IntelliJ projects currently are not supported.

Compiler plugins are uploaded on bintray: https://bintray.com/kotlin/kotlinx/kotlinx.serialization.plugin

## Building and usage

### Prerequisites:
Before all, follow the instructions from root README.md to download dependencies and build Kotlin compiler. (`ant -f update_dependencies.xml && ./gradlew dist`)

**Plugin works only with IntelliJIDEA 2017.2 and higher.**

Make sure you have latest dev version of Kotlin plugin installed.

### With gradle:

Run `./gradlew :kotlinx-serialization-compiler-plugin:dist`.
In IDEA, open `Settings - Plugins - Install plugin from disk...` and choose `$kotlin_root/dist/artifacts/Serialization/lib/kotlinx-serialization-compiler-plugin.jar`

### From within IDE (for development):

Run `./gradlew runIde` You'll get a fresh copy of IDEA with Kotlin and Kotlin-serialization plugins built from sources.

## Building gradle plugin

Run `./gradlew :kotlinx-gradle-serialization-plugin:install`

## Building maven plugin

Go to `$kotlin_root/libraries/tools/kotlin-maven-serialization`. Run `mvn install`

