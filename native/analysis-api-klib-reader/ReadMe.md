# Native Analysis Api based Klib reader

## Tasks

**Run Tests**

``` 
./gradlew :native:analysis-api-klib-reader:check
```

## Usage: Reading symbols from a given klib module

A set of top level `KlibDeclarationAddress` can be read from `KtLibraryModule` by

```kotlin
fun example(module: KtLibraryModule) {
    val addresses = module.readKlibDeclarationAddresses()
}
```

Such addresses can be resolved to `KtSymbol`s by

```kotlin
addresses.flatMap { address -> address.getSymbols() }
```

Note: `getSymbols` returns a sequence as multiple symbols can live under the same address.

## Usage: Getting all library modules in a Standalone Analysis API session. 

Example of creating a session
```kotin
 val session = buildStandaloneAnalysisAPISession {
     buildKtModuleProvider {
         // ... 
         addModule(buildKtLibraryModule {
             addBinaryRoot(pathToKlib)
             // ..
         })
     }
  }
```

Example analyzing libraries within the session
```kotlin
session.getAllLibraryModules().forEach { module -> 
    analyze(module) { 
        module.readKlibDeclarationAddresses()
            .flatMap { address -> address.getSymbols() }
    }
}
```

## Test Strategy

### Black Box Test:

We are building a klib from the `testProject`.
This klib will be used to read the addresses. Those addresses will be rendered and compared to
[!testProject.addresses](testData%2F%21testProject.addresses)

