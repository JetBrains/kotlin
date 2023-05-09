package app

import lib.*

fun runAppAndReturnOk(): String {
    if (prop != 1) error("prop is '$prop', but is expected to be '1'")
    val funcValue = func()
    if (funcValue != 2) error("func() is '$funcValue', but is expected to be '2'")
    val inlineFuncValue = inlineFunc()
    if (inlineFuncValue != 3) error("inlineFunc() is '$inlineFuncValue', but is expected to be '3'")
    if (constant != 4) error("constant is '$constant', but is expected to be '4'")

    return "OK"
}