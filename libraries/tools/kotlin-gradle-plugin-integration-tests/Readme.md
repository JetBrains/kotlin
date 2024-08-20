### Gradle Plugin Integration Tests

This module contains integration tests for main [`libraries/tools/kotlin-gradle-plugin`](../kotlin-gradle-plugin/ReadMe.md) plugin 
and other Gradle subplugins ('kapt', 'allopen', etc...).

#### How to run

To run all tests for all Gradle plugins use `check` task.

More fine-grained test tasks exist covering different parts of Gradle plugins:
- `kgpJvmTests` - runs all tests for Kotlin Gradle Plugin/Jvm platform (parallel execution)
- `kgpJsTests` - runs all tests for Kotlin Gradle Plugin/Js platform (parallel execution)
- `kgpAndroidTests` - runs all tests for Kotlin Gradle Plugin/Android platform (parallel execution)
- `kgpMppTests` - run all tests for Kotlin Gradle Multiplatform plugin (parallel execution)
- `kgpNativeTests` - run all tests for Kotlin Gradle Plugin with K/N (parallel execution)
- `kgpDaemonTests` - runs all tests for Gradle and Kotlin daemons (sequential execution)
- `kgpOtherTests` - run all tests for support Gradle plugins, such as kapt, allopen, etc (parallel execution)
- `kgpAllParallelTests` - run all tests for all platforms except daemons tests (parallel execution)

If you want to run only one test class, you need to append `--tests` flag with value of test class, which you want to run
```shell
./gradlew :kotlin-gradle-plugin-integration-tests:kgpAllParallelTests --tests <class-name-with-package>
```

#### How to Run Using Kotlin Native from Master

Currently, Kotlin Native from master involves three configurations: `kgpMppTests`, `kgpNativeTests`, and `kgpOtherTests`. 
Depending on your development environment, there are a few different ways you can run this.
* **On Local Environment** In the case of Local Environment builds, you have two options, which you can add in your `local.properties` file:  
  * `kotlin.native.enabled=true` - this property adds building Kotlin Native full bundle step before Integration Tests, then this bundle will be used in the Integration Tests.
  * `kotlin.native.local.distribution.for.tests.enabled=false` - include this line if you need to disable running Integration Tests with Kotlin/Native from master, even when `kotlin.native.enabled` is set to true.
* **On TeamCity** In the case of TeamCity builds, no extra setting is necessary. The mentioned configurations depend on the `full bundle`, which stores its artifacts in the `-DkonanDataDirForIntegrationTests` directory.
Also, you can specify the konan directory for test by providing `-DkonanDataDirForIntegrationTests`, for example:
```bash
 ./gradlew :kotlin-gradle-plugin-integration-tests:kgpNativeTests -DkonanDataDirForIntegrationTests=/tmp/.konan
```

#### How to work with the tests

Few rules you should follow while writing tests:
- All tests should be written using [JUnit 5 platform](https://junit.org/junit5/docs/current/user-guide/#overview).
- Consider writing tests for specific supported platform in plugin rather than for specific supported Gradle feature.  For example, 
if you want to add some tests for Gradle build cache, add them in the related test suites for Kotlin/Jvm, Kotlin/Js, etc...
- Don't create one big test suite (class). Consider splitting tests into smaller suites. All tests are running in parallel (except daemon tests)
and having small tests suites should improve overall tests running time.
- In tests consider using more specific tasks over general one. For example, use `assemble` instead of `build` when test does not need to also
compile tests and run them. This should reduce test execution time.
- By default, tests are running with `LogLevel.INFO` log level. Don't set `LogLevel.DEBUG` unless it is really required. Debug log level produces
a lot of output, that slows down test execution.
- Add `@DisplayName(...)` with meaningful description both for test class and methods inside. This will allow developers easier 
to understand what test is about.
- Add to test related Kotlin tag. For example for Kotlin/Jvm tests - `@JvmGradlePluginTests`. Other available tags are located nearby 
`@JvmGradlePluginTests` - check yourself what suites best for the test. You could add tag onto test suite once, but then all tests 
in test suite should be for the related tag. Preferably add tag for each test.
- Consider using [Gradle Plugin DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block) while adding new/modifying 
existing test projects.

Tests run using [Gradle TestKit](https://docs.gradle.org/current/userguide/test_kit.html) and may reuse already active Gradle TestKit daemon.
Shared TestKit caches are located in [./.testKitDir](.testKitDir) directory. It is cleared on CI after test run is finished, but not locally.
You could clean it locally by running `cleanTestKitCache` task.

#### How to debug Kotlin daemon

1. Create `Remote JVM debug` configuration in IDEA. 
   1. Modify debug port to be `5005`. 
   2. In `Debugger mode` floating menu select `Listen to remote JVM`. 
   3. (Optional) You can check `Auto restart` to automatically restart configuration after each debug session.
2. Specify correct debug port in `build` call arguments `kotlinDaemonDebugPort = 5005`.
3. Run newly created configuration in `Debug` mode and after that run test in simple `Run` mode.

##### Adding new test suite

Select appropriate tag [annotation](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/testTags.kt) and add it to the test class, 
so it will be assigned to the related test task. Extend test class from [KGPBaseTest](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/KGPBaseTest.kt).

For each test method add `@GradleTest` annotation and `gradleVersion: GradleVersion` method parameter.
All tests annotated with `@GradleTest` are [parameterized tests](https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests),
where provided parameter is Gradle version. By default, test will receive minimal and latest supported Gradle versions. It is possible 
to modify/add additional Gradle versions by adding `@GradleTestVersions` annotation either to the whole suite or to the specific test method.
Prefer using [TestVersions](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/TestVersions.kt) to define required versions instead of
writing them directly as String.

Use test DSL defined [here](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/testDsl.kt) to write actual test case:
```kotlin
project("someProject", gradleVersion) {
    build("assemble") {
        assertTasksExecuted(":compileKotlin")
    }
}
```

All test projects are located in [resources/testProject](src/test/resources/testProject) directory. You could use existing test projects
or add a new one. Test setup, on running the test, will automatically add [new](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/projectSetupDefaults.kt)
`settings.gradle` file or missing `pluginsManagement { ... }` block into existing file, so you could just use plugins without version
in build scripts:
```groovy
plugins {
    id "org.jetbrains.kotlin.jvm"
}
```

A bunch of additional useful assertions available to use, such as file assertions, output assertions and task assertions. If you want to
add a new assertion, add as a reviewer someone from Kotlin build tools team.

##### Additional test helpers

- Whenever you need to test combinations of different JDKs and Gradle versions - you could use `@GradleWithJdkTest` instead of `@GradleTest`. 
Then test method will receive requires JDKs as a second parameter:
```kotlin
@JdkVersions(version = [JavaVersion.VERSION_11, JavaVersion.VERSION_21])
@GradleWithJdkTest
fun someTest(
    gradleVersion: GradleVersion, 
    providedJdk: JdkVersions.ProvidedJdk
) {
    project("simple", gradleVersion, buildJdk = providedJdk.location) {
        build("assemble")
    }
}
```

- Whenever Android Gradle plugin different versions should be checked in the tests - it is possible to use `@GradleAndroidTest` annotation 
instead of `@GradleTest`. Test will receive additionally to Gradle version AGP version and required JDK version:
```kotlin
@AndroidTestVersions(additionalVersions = [TestVersions.AGP.AGP_42])
@GradleAndroidTest
fun someTest(
    gradleVersion: GradleVersion,
    agpVersion: String,
    jdkVersion: JdkVersions.ProvidedJdk
) {
    project(
        "simpleAndroid",
        gradleVersion,
        buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        buildJdk = jdkVersion.location
    ) {
        build("assembleDebug")
    }
}
```

- If you want to copy current state of the test project and play with it separately - you could use `makeSnapshotTo(destinationPath)` function.

##### Common test fixes

Test infrastructure adds following common fixes to all test projects:
- applies 'org.jetbrains.kotlin.test.fixes.android' [plugin](../gradle/android-test-fixes/Readme.md). If you are using custom `settings.gradle`
or `settings.gradle.kts` content in the test project, you need to add this plugin into `pluginManagement`:
<details open>
<summary>Kotlin script</summary>

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
    }

    val test_fixes_version: String by settings
    plugins {
       id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
    }
}
```
</details>
<details>
<summary>Groovy</summary>

```groovy
pluginManagement {
    repositories {
        mavenLocal()
    }
    
    plugins {
       id "org.jetbrains.kotlin.test.fixes.android" version $test_fixes_version
    }
}
```
</details>

## build.gradle.kts injections from main test code

It is possible to inject code from IT test directly into the build files of the test project.
To do that use `buildScriptInjection` DSL function as follows:

```kotlin
nativeProject("native-fat-framework/smoke", gradleVersion) {
    buildScriptInjection {
        // This code will be executed inside build.gradle.kts during project evaluation
        val macos = kotlinMultiplatform.macosX64()
        macos.binaries.framework("DEBUG")
        val fat = project.tasks.getByName("fat") as FatFrameworkTask
        fat.from(macos.binaries.getFramework("DEBUG"))
    }
    buildAndFail("assemble") {}
}
```

It is possible to inject the same code to both groovy and kts buildscript files. 

Invocation of `buildGradleKtsInjection` adds  to the build script classpath classes from test. And injects in build script this line:
`org.jetbrains.kotlin.gradle.testbase.invokeBuildScriptInjection(project, "<FQN to class produced from lambda>")`