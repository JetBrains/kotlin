# Artifacts Tests

This module contains tests for changes in Maven and Gradle metadata for all maven artifacts we publish, 
as well as verifies the contents of the Kotlin distribution

To reproduce locally build all artifacts first:

```shell
./gradlew clean install publish mvnPublish dist -Pteamcity=true -PdeployVersion=99.0.255-KotlinVersionPlaceholder
```

`-Pteamcity=true` and `-PdeployVersion=99.0.255-KotlinVersionPlaceholder` are needed for Gradle metadata tests.

* `-Pteamcity=true` is needed because the tests expect artifacts to have metadata for Javadoc elements, which are only generated on CI.
* `deployVersion` has to be overridden with a version, that doesn't have the word `SNAPSHOT` in it,
so that the attribute `org.gradle.status` in the resulting metadata has value `release` instead of `integration`.

`deployVersion` should also be overridden when running Gradle metadata tests. 
Either add `deployVersion=99.0.255-KotlinVersionPlaceholder` to `gradle.properties` or run the tests using the following command:

```shell
./gradlew :repo:artifacts-tests:test --tests "org.jetbrains.kotlin.code.GradleMetadataTest" -PdeployVersion=99.0.255-KotlinVersionPlaceholder
```

## Generating test data for the Gradle metadata test

To generate Gradle metadata template, build artifacts using the command above, then run the following from the root of the repository.

```shell
./repo/artifacts-tests/generate-gradle-metadata-template.sh build/repo/org/jetbrains/kotlin/ repo/artifacts-tests/src/test/resources/org/jetbrains/kotlin/ '99.0.255-KotlinVersionPlaceholder' 'ArtifactsTest.version'
```