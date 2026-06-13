// FULL_JDK
// LANGUAGE: +CompanionBlocksAndExtensions
// FIR_DUMP

import lombok.extern.java.Log

@Log
class C {
    companion {
        val log = "OK"

        fun test() {
            Companion.log.info("test")
        }
    }
}

fun box(): String {
    C.test()
    return C.log
}
