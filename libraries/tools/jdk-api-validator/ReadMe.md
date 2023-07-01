# Verifies that a library compiled with a newer JDK is compatible with older JDK API

Currently, the project only checks `kotlin-reflect` for JDK 1.6 compatibility.
The check is necessary to make sure applications using `kotlin-reflect` can run on older Android devices.

## How to run

Run from the root directory of the kotlin project:

`./gradlew :tools:jdk-api-validator:test`

## How to interpret the result

Successful completion of the `test` task means the checked libraries are compatible with JDK 1.6 API.
In case of failure, the exact location and name of the violating API references are logged as error.

An example of validation error:

`[ERROR] /kotlin/libraries/reflect/build/libs/kotlin-reflect-1.9.255-SNAPSHOT.jar:kotlin/reflect/jvm/internal/ComputableClassValue.class:47: Undefined reference: void ClassValue.<init>()`

## How to fix a failure

If the violating reference can be desugared by R8 or its execution is prevented on Android platform,
the error can be suppressed. See `suppressAnnotations` and `undefinedReferencesToIgnore` in `JdkApiUsageTest.kt`.
Otherwise, the API should be avoided.

