package app

import lib.J

fun runAppAndReturnOk(): String {
    return if (J::class.java.annotations.size == 1) "OK" else "Fail"
}
