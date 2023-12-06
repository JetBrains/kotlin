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
./gradlew :native:objcexport-header-generator:check
```

The most important test is [ObjCExportHeaderGeneratorTest.kt](test%2Forg%2Fjetbrains%2Fkotlin%2Fbackend%2Fkonan%2Ftests%2FObjCExportHeaderGeneratorTest.kt)
as this test defines the contract of how a header shall be generated from a given Kotlin input. This test can run against 
both implementations. 

```
./gradlew :native:objcexport-header-generator:testK1
./gradlew :native:objcexport-header-generator:testAnalysisApi
```

Note: Since the Analysis Api implementation is WIP yet, this test can be used for debugging, but is not fully implemented yet.
