# ObjC Export Header generator
This tool is used for 'translating' Kotlin code into ObjC 'stubs' and ultimately rendering ObjC header files.

## Usage

### CLI: Building .framework files
The CLI will use this module for building the corresponding .framework binaries from Kotlin. This method will operate on previously
built .klib binaries (with fully validated frontend). The 'klibs' will be deserialized and headers and bridges between
Kotlin and ObjC will be built. This mode currently operates on K1 based descriptors

### IDE: Providing Kotlin <-> ObjC/Swift cross language support
In order for Fleet to provide tooling that is capable of refactoring symbols between Kotlin and ObjC, this tool is used. 

Example:

given the following Kotlin code

```kotlin
@ObjCName("FooObjC")
class Foo
```

and the following Swift usage

```swift
func bar() {
    FooObjC()
}
```
refactoring inside either Kotlin or Swift will be consistent across Kotlin and Swift.


## Two Implementations (K1, Analysis Api)

There are currently two implementations for this tool

### K1
This is the K1 (descriptor based) implementation that is currently used by the CLI and K1 based IDEs. 

### Analysis Api (WiP)
This implementation is currently 'work in progress' and shall replace the K1 usage in the IDE later. 
This implementation _could_ theoretically also replace the K1 implementation if necessary.


## Testing

### Run all tests
```
./gradlew :native:objcexport-header-generator:check --continue
```

The most important test is [ObjCExportHeaderGeneratorTest.kt](test%2Forg%2Fjetbrains%2Fkotlin%2Fbackend%2Fkonan%2Ftests%2FObjCExportHeaderGeneratorTest.kt)
as this test defines the contract of how a header shall be generated from a given Kotlin input. This test can run against 
both implementations. 

```
./gradlew :native:objcexport-header-generator:testK1 --continue
./gradlew :native:objcexport-header-generator:testAnalysisApi --continue
```

Note: Since the Analysis Api implementation is WIP yet, this test can be used for debugging, but is not fully implemented yet.

### How tests work on TC

On TC all tests are called by `./gradlew check`. ObjCExport module has 2 groups of tests:
- K1
- K2
  They use different classpaths to include different header generators:
```kotlin
objCExportHeaderGeneratorTest("testK1", testDisplayNameTag = "K1") {
    classpath += k1TestRuntimeClasspath
    exclude("**/ObjCExportIntegrationTest.class")
}

objCExportHeaderGeneratorTest("testAnalysisApi", testDisplayNameTag = "AA") {
    classpath += analysisApiRuntimeClasspath
    exclude("**/ObjCExportIntegrationTest.class")
}
```
Also we configure order of execution:
```kotlin
objCExportHeaderGeneratorTest("testIntegration") {
    mustRunAfter("testK1", "testAnalysisApi")
}
```
So, when TC calls `./gradlew check` this happens:
1. `GenerateObjCExportIntegrationTestData` called with K1 classpath and generated K1 header is stored `build` directory
2. `GenerateObjCExportIntegrationTestData` called with K2 classpath and generated K2 header is stored `build` directory
3.  `testIntegration` calls `ObjCExportIntegrationTest` which builds K1 and K2 indexes and compares them.

### CI setup and 'TodoAnalysisApi'
As explained previously, tests in :native:objcexport-header-generator will be able to run against K1 and the AA implementation. 
The CI will now run both cases. However, some tests are not yet expected to pass for the newer AA based implementation. 
In this case the test can be marked as 'todo' using the `@TodoAnalysisApi` annotation. 

Example 
```kotlin
@Test
@TodoAnalysisApi
fun myTest() {
    
}
```

This annotation will
- Ignore test the test failure for the AA based implementation on the CI
- Mark the displayName of the test with 'TODO' (e.g. `[AA] myTest // TODO`)

Note:
- If the annotation is still present, but the test is successful, then an error is emitted that reminds you about removing the annotation
- To make the tests execute normally (for more convenient local development), Gradle property `kif.local` can be used:
```text
./gradlew :native:objcexport-header-generator:check -Pkif.local
                                                  //  ^
```

## Integration tests

Currently K1 and K2 versions of ObjCExport are used together in IDE, so it's important to keep eye on structural equality of generated
headers by both versions. Here is an example of how headers might be valid, but cause issues in IDE:

```kotlin
interface ValueStorage {
    fun storeValue(value: Boolean)
    fun storeValue(value: String)
}
```

```c
@protocol ValueStorage
- (void)storeValue:(BOOL)value __attribute__((swift_name("storeValue(value:)")));
- (void)storeValue_:(NSString)value __attribute__((swift_name("storeValue(value_:)")));
@end
```

```c
@protocol ValueStorage
- (void)storeValue:(BOOL)value __attribute__((swift_name("storeValue(value:)")));
- (void)storeValue__:(NSString)value __attribute__((swift_name("storeValue(value__:)")));
@end
```

Both headers are valid, but because of mangling bug `swift_name` attributes are different. So user can pass parameter
with name `value_` in Swift and code is going to be green, but at compile time there will be error about absence of parameter `value_`.

To verify structural differences:

1. We generate headers
   in [GenerateObjCExportIntegrationTestData](test/org/jetbrains/kotlin/backend/konan/tests/integration/GenerateObjCExportIntegrationTestData.kt)
2. Then compile both headers with `Indexer` and compare indexer result
   in [ObjCExportIntegrationTest](test/org/jetbrains/kotlin/backend/konan/tests/integration/ObjCExportIntegrationTest.kt)

### To run/debug integration test

1. `./gradlew :native:objcexport-header-generator:check --continue` to generate test data (can also be done from IDE by calling K1 and AA
   test groups
   on [GenerateObjCExportIntegrationTestData](test/org/jetbrains/kotlin/backend/konan/tests/integration/GenerateObjCExportIntegrationTestData.kt))
2. Run test [ObjCExportIntegrationTest](test/org/jetbrains/kotlin/backend/konan/tests/integration/ObjCExportIntegrationTest.kt) in IDE in
   debug mode
3. Instance of [IntegrationTestReport](test/org/jetbrains/kotlin/backend/konan/tests/integration/utils/IntegrationTestReport.kt) is going to
   be created and can be inspected in debugger. 