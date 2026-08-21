// FULL_JDK

// MODULE: lib
// FILE: lib.kt

// Its own package, so that its loggers cannot collide with another test file's - see `log.kt`.
package fromLibrary

import lombok.extern.java.Log
import lombok.AccessLevel

@Log(access = AccessLevel.PUBLIC)
class LogExample

// MODULE: main(lib)
// FILE: main.kt

package fromLibrary

fun box(): String {
    LogExample.log.info("Check @Log on class from library")
    return "OK"
}
