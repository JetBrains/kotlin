### Gradle Plugin Integration Tests

This module contains integration tests for [`libraries/tools/kotlin-gradle-plugin`](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-gradle-plugin) (and the subplugins mentioned there).

#### How to run

There are three Gradle tasks that run the tests:

* Run all tests with the oldest possible Gradle version for each test:

      ./gradlew :kotlin-gradle-plugin-integration-tests:test
    
* Run with the new Gradle release, choose only the tests that support this Gradle version:

      ./gradlew :kotlin-gradle-plugin-integration-tests:testAdvanceGradleVersion
    
* Run the incremental compilation tests generated from the JPS ones

      ./gradlew :kotlin-gradle-plugin-integration-tests:testFromJps
    
The tests that use the Gradle plugins DSL ([`PluginsDslIT`](../kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/PluginsDslIT.kt)) also require the Gradle plugin marker artifacts to be installed:

    ./gradlew -Pdeploy_version=1.2-test :kotlin-gradle-plugin:plugin-marker:install :kotlin-noarg:plugin-marker:install :kotlin-allopen:plugin-marker:install
    ./gradlew -Pdeploy_version=1.2-test :kotlin-gradle-plugin-integration-tests:test
    
#### How to work with the tests

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
