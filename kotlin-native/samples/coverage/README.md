# Code Coverage usage sample

This example shows how to collect coverage information during execution of the test suite. 
Please note that this functionality will be incorporated into Gradle plugin so you won't need to do it by hand in the nearest future. 

### Prerequisites
`createCoverageReport` task requires `llvm-profdata` and `llvm-cov` to be added to the `$PATH`. 
They can be found in the Kotlin/Native dependencies dir. By default it should look like
`$HOME/.konan/dependencies/clang-llvm-6.0.1-darwin-macos/bin`

### Usage
Just run `createCoverageReport` task.
