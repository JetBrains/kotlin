# Artifacts Tests

This module contains tests for changes in Maven and Gradle metadata for all maven artifacts we publish, 
as well as verifies the contents of the Kotlin distribution

To reproduce locally, first run:

```shell
./gradlew clean install publish mvnPublish dist -Pteamcity=true
```

`-Pteamcity=true` is needed for Gradle metadata tests because the tests expect artifacts to contain metadata for Javadoc elements, 
which are only generated on CI.
