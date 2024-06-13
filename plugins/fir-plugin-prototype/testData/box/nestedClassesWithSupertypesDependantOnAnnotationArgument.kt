// DUMP_IR
// FIR_DUMP

import org.jetbrains.kotlin.fir.plugin.AddNestedClassesBasedOnArgument

interface A {
    fun test(): String = "OK"
}

@AddNestedClassesBasedOnArgument(A::class)
interface Some {
    /*
    interface Generated : A
     */
}

fun box(): String {
    val x = object : Some.Generated {}
    return x.test()
}
