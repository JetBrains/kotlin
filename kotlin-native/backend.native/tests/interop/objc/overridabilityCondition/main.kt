@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.ObjCClass

@Suppress("CONFLICTING_OVERLOADS")
class OverrideAll : ObjCClass() {
    override fun fooWithArg(arg: Int, arg2: String?) = "D"
    override fun fooWithArg(ohNoOtherName: Int, name2: String?) = "E"
    override fun fooWithArg(arg: Int, name3: String?) = "F"
}

@Suppress("CONFLICTING_OVERLOADS")
class OverrideNone : ObjCClass() {
}

@Suppress("CONFLICTING_OVERLOADS")
class OverrideOne : ObjCClass() {
    override fun fooWithArg(arg: Int, arg2: String?) = "G"
}

@Suppress("CONFLICTING_OVERLOADS")
class OverrideWithDifferentFirstArgName : ObjCClass() {
    override fun fooWithArg(a: Int, arg2: String?) = "H"
    override fun fooWithArg(b: Int, name2: String?) = "I"
    override fun fooWithArg(c: Int, name3: String?) = "J"
}

fun test(x: ObjCClass, expected: String) {
    val res = x.fooWithArg(arg = 0, arg2 = "") +
            x.fooWithArg(ohNoOtherName = 0, name2="") +
            x.fooWithArg(arg = 0, name3 = "")
    if (res != expected) throw IllegalStateException("Fail ${x::class}: ${res} instead of $expected")
}

fun main() {
    test(ObjCClass(), "ABC")
    test(OverrideAll(), "DEF")
    test(OverrideNone(), "ABC")
    test(OverrideOne(), "GBC")
    test(OverrideWithDifferentFirstArgName(), "HIJ")

//  Also test non-virtual calls
    val x1 = OverrideAll()
    val res1 = x1.fooWithArg(arg = 0, arg2 = "") +
            x1.fooWithArg(ohNoOtherName = 0, name2="") +
            x1.fooWithArg(arg = 0, name3 = "")
    if (res1 != "DEF") throw IllegalStateException("Fail OverrideAll non-virtual: ${res1} instead of DEF")

    val x2 = OverrideNone()
    val res2 = x2.fooWithArg(arg = 0, arg2 = "") +
            x2.fooWithArg(ohNoOtherName = 0, name2="") +
            x2.fooWithArg(arg = 0, name3 = "")
    if (res2 != "ABC") throw IllegalStateException("Fail OverrideNone non-virtual: ${res2} instead of ABC")
}