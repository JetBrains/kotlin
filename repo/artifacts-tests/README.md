# Artifacts Tests

This module contains tests for changes in pom files for all maven artifacts we publish

To reproduce locally build all artifacts first:

```shell
# clean local m2 from old artifacts to avoid unrelated failures
# up-to-date version is in defaultSnapshotVersion property and should be used instead of "*-2.0.255*"
find ~/.m2/repository/org/jetbrains/kotlin -name "*-2.0.255*" -delete

./gradlew install
cd libraries
./mvnw install -DskipTests
```
