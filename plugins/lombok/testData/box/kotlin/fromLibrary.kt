// FULL_JDK

// MODULE: lib
// FILE: lib.kt

import lombok.extern.java.Log
import lombok.AccessLevel

@Log(access = AccessLevel.PUBLIC)
class LogExample

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    LogExample.log.info("Check @Log on class from library")
    return "OK"
}
