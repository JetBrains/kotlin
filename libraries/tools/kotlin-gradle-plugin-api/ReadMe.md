## Gradle Plugin Api

Lightweight module defining the API surface of the Kotlin Gradle Plugin. 

### Binary Compatibility Validation
This module is tested for Binary Compatibility Validation by the :tools:binary-compatibility-validator module. 
Please see [ReadMe.md](../binary-compatibility-validator/ReadMe.md)

You can execute binary compatibility validation by a shared run-configuration in the IDE
`/Tests/Test: binary compatibility` 

In order to check in changes you can launch 
`Test/Test: binary compatibility [overwrite]`