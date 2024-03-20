# Module binary-compatibility-validator

Binary compatibility validator allows dumping Kotlin library ABI (both for JVM libraries and KLibs) 
that is public in the sense of Kotlin visibilities and ensures that the public ABI
wasn't changed in a way that makes this change binary incompatible.

# Package kotlinx.validation

Provides common declarations, Gradle plugin tasks and extensions. 

# Package kotlinx.validation.api

Provides an API for dumping Kotlin Java libraries public ABI.

# Package kotlinx.validation.api.klib

Provides an API for dumping Kotlin libraries (KLibs) public ABI and managing resulting dumps.

**This package is experimental, both API and behaviour may change in the future. There are also no guarantees on preserving the behavior of the API until its
stabilization.**
