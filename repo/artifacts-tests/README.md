# Artifacts Tests

This module contains tests for changes in pom files for all maven artifacts we publish

To reproduce locally build all artifacts first:

```shell
# clean local m2 from old artifacts to avoid unrelated failures
find ~/.m2/repository/org/jetbrains/kotlin -name "*-1.9.255*" -delete

./gradlew install
cd libraries
mvn install -DskipTests
```
