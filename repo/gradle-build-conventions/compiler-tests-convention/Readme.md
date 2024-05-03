# compiler-test-convention

## Description

This plugin adds dependencies to the test tasks, building the required jars and tracking them as inputs, but it passes the absolute paths to the test as systemProperties. 

This plugin is key for:
- make tests cacheable, avoiding execution if no input changed
- allow Gradle Predictive Test Selection to understand the code changes and have an accurate prediction

## Usage

Apply it: 
```kotlin
plugins {
    id("compiler-test-convention")
}
```

Add dependencies to the test classpath (`kotlin-stdlib`, `kotlin-stdlib-jvm-minimal-for-test`, and `kotlin-reflect` are always added):
```kotlin
compilerTests {
    withStdlibCommon()
    withTestJar()
    withAnnotations()
    ...
}
```
