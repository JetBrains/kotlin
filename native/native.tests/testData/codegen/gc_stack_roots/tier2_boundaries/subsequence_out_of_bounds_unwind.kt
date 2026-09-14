// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true

@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.runtime.GC

class SimpleRoot(val tag: String, var ref: SimpleRoot? = null)

fun box(): String {
    val root = SimpleRoot("AliveBeforeUnwind")
    var caught = false
    try {
        val str = "Hello, world!"
        val sub = (str as CharSequence).subSequence(-1, 5)
        return "FAIL: should have thrown but returned $sub"
    } catch (e: IndexOutOfBoundsException) {
        caught = true
    } catch (e: Throwable) {
        return "FAIL: unexpected exception type: ${e::class.simpleName}"
    }

    if (!caught) return "FAIL: not caught"

    GC.collect()

    if (root.tag != "AliveBeforeUnwind") return "FAIL: root corrupted"

    return "OK"
}
