## Gradle Plugin Api

Lightweight module defining the API surface of the Kotlin Gradle Plugin. 

### Binary Compatibility Validation

The public API surface of this module is checked for stability
using the [binary compatibility validator](https://github.com/Kotlin/binary-compatibility-validator/) plugin
to prevent accidental public API changes.

You can execute public API validation by running `apiCheck` task (also executed when `check` task runs).

In order to overwrite the reference API snapshot, you can launch `apiDump` task. 
