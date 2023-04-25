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
- `kgpDaemonTests` - runs all tests for Gradle and Kotlin daemons (sequential execution)
- `kgpOtherTests` - run all tests for support Gradle plugins, such as kapt, allopen, etc (parallel execution)
- `kgpAllParallelTests` - run all tests for all platforms except daemons tests (parallel execution)

Also, few deprecated tasks still exist until all tests will be migrated to the new setup:
- `kgpSimpleTests` - runs all migrated Kotlin Gradle Plugin tests (parallel execution)
- `test` - runs all tests with the oldest supported Gradle version (sequential execution)
- `testAdvancedGradleVersion` - runs all tests with the latest supported Gradle version (sequential execution)

The old tests that use the Gradle plugins DSL ([`PluginsDslIT`](../kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/PluginsDslIT.kt)) 
also require the Gradle plugin marker artifacts to be installed:
```shell
./gradlew :kotlin-gradle-plugin:plugin-marker:install :kotlin-noarg:plugin-marker:install :kotlin-allopen:plugin-marker:install
./gradlew :kotlin-gradle-plugin-integration-tests:test
```

If you want to run only one test class, you need to append `--tests` flag with value of test class, which you want to run
```shell
./gradlew :kotlin-gradle-plugin-integration-tests:kgpAllTests --tests <class-name-with-package>
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

- Whenever you need to test combination of different JDKs and Gradle versions - you could use `@GradleWithJdkTest` instead of `@GradleTest`. 
Then test method will receive requires JDKs as a second parameter:
```kotlin
@JdkVersions(version = [JavaVersion.VERSION_11, JavaVersion.VERSION_17])
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

##### Deprecated tests setup

When you create a new test, figure out which Gradle versions it is supposed to run on. Then, when you instantiate a test project, specify one of:

* `project("someProjectName", GradleVersionRequired.None)` or just `project("someProjectName")` – the test can run on the whole range of the supported Gradle versions;
* `project("someProjectName", GradleVersionRequired.AtLeast("X.Y"))` – the test is supposed to run on Gradle version `X.Y` and newer (e.g. it tests integration with a Gradle feature that was released in version `X.Y`);
* `project("someProjectName", GradleVersionRequired.Exact("X.Y"))` – the test is supposed to run only with Gradle version `X.Y` (e.g. it tests a workaround for that version or records some special behavior that is not reproducible with newer versions).

:warning: When your tests target multiple Gradle versions, make sure they pass when run with both tasks `test` and `testAdvanceGradleVersion` (see above). In the IDE, you can modify a test run configuration to use a Gradle task other than `test`.

You can check a Gradle version that the test runs with using [`Project.testGradleVersionAtLeast("X.Y")`](https://github.com/JetBrains/kotlin/blob/fe3ce1ec7cdd29a1839f3dd67e0a00023efa495d/libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/BaseGradleIT.kt#L454) and [`Project.testGradleVersionBelow("X.Y")`](https://github.com/JetBrains/kotlin/blob/fe3ce1ec7cdd29a1839f3dd67e0a00023efa495d/libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/BaseGradleIT.kt#L457).

Since Gradle output layouts differ from version to version, you can access classes and resources output directories using the functions that adapt to the Gradle version that is used for each test:

* [`CompiledProject.kotlinClassesDir()`](https://github.com/JetBrains/kotlin/blob/fe3ce1ec7cdd29a1839f3dd67e0a00023efa495d/libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/BaseGradleIT.kt#L459) with optional arguments for subproject and source set, and its Java counterpart [`CompiledProject.javaClassesDir()`](https://github.com/JetBrains/kotlin/blob/fe3ce1ec7cdd29a1839f3dd67e0a00023efa495d/libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/BaseGradleIT.kt#L462) (note that Gradle versions below 4.0 use the same directory for both)

* [`Project.resourcesDir()`](https://github.com/JetBrains/kotlin/blob/fe3ce1ec7cdd29a1839f3dd67e0a00023efa495d/libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/BaseGradleIT.kt#L444) (with optional arguments for subproject and source set) for the resources directory;

* [`Project.classesDir()`](https://github.com/JetBrains/kotlin/blob/fe3ce1ec7cdd29a1839f3dd67e0a00023efa495d/libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/BaseGradleIT.kt#L449), which is a general way to get the output directory for a specific subproject, source set, and language.
