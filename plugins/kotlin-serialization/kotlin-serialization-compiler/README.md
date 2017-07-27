# Kotlin serialization IDEA plugin

Kotlin serialization plugin consists of three parts: a gradle compiler plugin, an IntelliJ plugin and a runtime library.
This is the IDEA plugin. Gradle plugin can be found in `libraries` folder.

Please note that this plugin currently works only for highlighting and resolving symbols, and it doesn't work with embedded IDEA compiler.
To build any project with serialization within IDEA, you'll need to delegate all build actions to gradle:
`File - Settings - Build, Execution, Deployment - Build Tools - Gradle - Runner -` tick `Delegate IDE build/run actions to gradle`. 
Maven and IntelliJ projects currently are not supported.

## Building and usage

### Prerequisites:
Before all, follow the instructions from root README.md to download dependencies and build Kotlin compiler. (`ant -f update_dependencies.xml && ant dist`)

**Plugin works only with IntelliJIDEA 2017.2 and higher.**

Make sure you have latest dev version of Kotlin plugin installed:
Open `Settings - Plugins - Browse Repositories... - Manage repositories` and add `https://teamcity.jetbrains.com/guestAuth/repository/download/Kotlin_master_CompilerAndPlugin_NoTests/.lastSuccessful/updatePlugins-IJ2017.2.xml`.
Update Kotlin plugin from new repository.

### With gradle:

Run `./gradlew buildPlugin`. 
In IDEA, open `Settings - Plugins - Install plugin from disk...` and choose `build/distributions/Kotlin-serialization-0.1-SNAPSHOT.zip`

### From within IDE (for development):

Open whole Kotlin project. Choose run configuration `IDEA` and run it. You'll get a fresh copy of IDEA with Kotlin and Kotlin-serialization plugins built from sources.

