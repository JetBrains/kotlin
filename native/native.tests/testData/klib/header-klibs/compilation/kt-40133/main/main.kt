package app

import lib.*

fun runAppAndReturnOk(): String {
    var result = "Fail"
    anInlineFunction { result = "OK" }
    return result
}
