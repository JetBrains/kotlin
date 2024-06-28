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

To check if an API can be desugared by R8:
1. Identify the earliest R8 version that supports current Kotlin version [here](https://developer.android.com/build/kotlin-support).
2. Download the R8 version jar artifact from Google's maven repository [here](https://maven.google.com/web/index.html#com.android.tools:r8).
3. Run it using the `BackportedMethodList` entry point, e.g., `java -cp r8-8.0.40.jar com.android.tools.r8.BackportedMethodList`.
4. Check if the violating API reference is in the printed list of methods.

Also, you can get the list of backported methods the downloaded version supports for a given Android API level:
```shell
$ java -cp r8-8.0.40.jar com.android.tools.r8.BackportedMethodList --help
Usage: BackportedMethodList [options]
 Options are:
  --output <file>         # Output result in <file>.
  --min-api <number>      # Minimum Android API level for the application
  --desugared-lib <file>  # Desugared library configuration (JSON from the
                          # configuration)
  --lib <file>            # The compilation SDK library (android.jar)
  --version               # Print the version of BackportedMethodList.
  --help                  # Print this message.
```

