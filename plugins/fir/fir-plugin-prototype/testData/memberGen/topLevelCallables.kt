package foo

import org.jetbrains.kotlin.fir.plugin.A

/*
 * Plugin generates `dummyClassName(value: ClassName): String` function for each class annotated with @A
 */

@A
class MySuperClass {
    fun test() {
        val s = dummyMySuperClass(this)
        takeString(s)
    }
}

fun takeString(s: String) {}
