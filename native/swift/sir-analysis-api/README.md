# Build Swift IR from Analysis API

This module is responsible for populating SIR tree. It is the first step in Swift Export pipeline.

Input:
It should be possible to populate SIR from two types of artefacts:
1/ Kotlin Source Module
2/ KLib (currently not supported)

## Dev guide

### How to generate tests:
```bash
./gradlew :generators:sir-analysis-api-generator:generateSirAnalysisApiTests
```
this will generate test by their input files. Input files could be found and should be placed here - `native/swift/sir-analysis-api/testData`

The test expects to find `.sir` file, containing serialized SIR for the test-case. Name of the `.sir` file should be the same as a name of corresponding `.kt` file.

The project for the generator can be found here - `generators/sir-analysis-api-generator/build.gradle.kts`

### How to run tests:
```bash
./gradlew :native:swift:sir-analysis-api:test --tests "*"
```
OR just open `SirAnalysisGeneratedTests` in IDEA and start them from gutter.

### Project Setup
No additional setup required to develop this project.
