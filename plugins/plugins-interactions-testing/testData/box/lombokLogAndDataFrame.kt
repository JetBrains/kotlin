// ISSUE: KT-90741
// FULL_JDK
// ENABLE_LOMBOK
// ENABLE_DATAFRAME
// FIR_DUMP

import lombok.extern.java.Log

@Log
class A

fun box(): String {
    A()
    return "OK"
}
