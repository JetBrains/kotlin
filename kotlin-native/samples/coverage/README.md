# Code Coverage usage sample

This example shows how to collect coverage information during execution of the test suite. 
Please note that this functionality will be incorporated into Gradle plugin so you won't need to do it by hand in the nearest future. 

### Prerequisites
`createCoverageReport` task requires `llvm-profdata` and `llvm-cov` to be added to the `$PATH`.  
In case of macOS, use tools from Xcode (`/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin`).  
For Windows and Linux, use the ones from Kotlin/Native LLVM distribution (e.g. `$HOME/.konan/dependencies/clang-llvm-8.0.0-linux-x86-64/bin`).
### Usage
Just run `createCoverageReport` task.
