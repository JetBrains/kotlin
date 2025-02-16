// TODO: string deduplication across several components seems to require
// linking them as bitcode modules before translating to machine code.
// IGNORE_BACKEND: JVM_IR
// DISABLE_NATIVE
// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val str1 = ""
    val str2 = "hello".subSequence(2, 2)
    if(!(str1 == str2))
        return "FAIL =="
    if(!(str1 === str2))
        return "FAIL ==="

    return "OK"
}
