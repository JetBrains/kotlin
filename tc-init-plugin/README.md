# TeamCity Init Plugin (`tc-init-plugin`)

This standalone Gradle plugin encapsulates the TeamCity Gradle runner logic and lifecycle listeners into a precompiled binary JAR (`teamcity-init-plugin.jar`).

Compiling the init script into a precompiled plugin provides maximum build efficiency:
- Eliminates dynamic Groovy script compilation overhead and runtime Metaspace/heap churn in large multi-project builds.
- Provides static bytecode execution for test output listeners, compile error streaming, and build problem reporting.
- Enables seamless compatibility with Gradle Configuration Cache.

---

## Architecture & Workflow

1. **Plugin Source:**
   - `tc-init-plugin/src/main/groovy/org/jetbrains/teamcity/TeamCityInitPlugin.groovy`: Contains all plugin logic, service message generators, build problem reporters, and test lifecycle interceptors.
   - `tc-init-plugin/build.gradle`: Standalone build configuration using Gradle's `java-gradle-plugin` and `groovy` plugins.

2. **Precompiled Artifact:**
   - `teamcity-init-plugin.jar`: The binary JAR containing the compiled plugin classes and plugin descriptors.
   - Located at the root of the project (`teamcity-init-plugin.jar`) and in `tc-init-plugin/`.

3. **Initialization Script:**
   - `tc-init_v2.gradle`: Lightweight bootstrap init script passed to Gradle invocations (via `-I tc-init_v2.gradle`). It resolves `teamcity-init-plugin.jar` and applies `org.jetbrains.teamcity.TeamCityInitPlugin`.

---

## How to Build and Update the JAR

When modifying the plugin source code in `tc-init-plugin/src/...`, recompile and update the precompiled JAR:

```bash
# 1. Compile and build the plugin JAR
./gradlew -p tc-init-plugin jar

# 2. Copy the updated JAR to the repository root
cp tc-init-plugin/build/libs/teamcity-init-plugin.jar teamcity-init-plugin.jar
cp tc-init-plugin/build/libs/teamcity-init-plugin.jar tc-init-plugin/teamcity-init-plugin.jar
```

Commit the updated source files alongside `teamcity-init-plugin.jar`.

---

## Usage in Gradle Builds / CI

To run Gradle builds with the TeamCity initialization logic:

```bash
./gradlew -I tc-init_v2.gradle -Dteamcity.internal.gradle.runner.doNotPopulateGradleProperties=true <tasks>
```
