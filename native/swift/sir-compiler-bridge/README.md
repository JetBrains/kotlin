# Swift IR to Kotlin compiler bridges

This module is responsible for the generation of "glue" code between Swift Export and Kotlin compiler.

Swift does not "know" how to call Kotlin directly. But it has a C/C++/Objective-C interop, and we can leverage it for Swift Export.

We do the following:
1. Generate a Kotlin wrapper around the original Kotlin function with a primitive C-like ABI.
2. Generate a C header with C declarations of these wrappers.
3. Call these C functions from Swift functions.

That's it! This approach also has the following advantages:
1. All nuances of Kotlin compiler and Kotlin/Native runtime are hidden behind wrappers with a simple ABI. 
So Swift Export (mostly) does not need to care about how Kotlin/Native works under the hood.
2. The contract between Swift Export and Kotlin compiler is well-defined in the form of a set of compiler intrinsics.

For the sake of simplicity, Kotlin wrappers are generated as Kotlin sources, 
but later we can switch to direct generation of Kotlin Backend IR.