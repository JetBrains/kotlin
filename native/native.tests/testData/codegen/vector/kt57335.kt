// TARGET_BACKEND: NATIVE
// WITH_PLATFORM_LIBS
// FREE_COMPILER_ARGS: -opt-in=kotlin.ExperimentalStdlibApi,kotlinx.cinterop.ExperimentalForeignApi

import kotlinx.cinterop.*
import kotlin.test.*

value class Vector3(val data: Vector128) {
    constructor(x: Float, y: Float, z: Float) : this(vectorOf(x, y, z, 0f))

    val x: Float get() = data.getFloatAt(0)
    val y: Float get() = data.getFloatAt(1)
    val z: Float get() = data.getFloatAt(2)

    operator fun plus(v: Vector3): Vector3 = Vector3(this.x + v.x, this.y + v.y, this.z + v.z)
    override fun toString(): String = "Vector3(${x}, ${y}, ${z})"
}

value class Vector4(val data: Vector128) {
    constructor(x: Float, y: Float, z: Float, w: Float) : this(vectorOf(x, y, z, w))

    val x: Float get() = data.getFloatAt(0)
    val y: Float get() = data.getFloatAt(1)
    val z: Float get() = data.getFloatAt(2)
    val w: Float get() = data.getFloatAt(3)

    operator fun plus(v: Vector4): Vector4 = Vector4(this.x + v.x, this.y + v.y, this.z + v.z, this.w + v.w)
    override fun toString(): String = "Vector4(${x}, ${y}, ${z}, ${w})"
}

var globalAnySink: Any? = null

fun doMathAndForceBoxing() {
    val v3 = Vector3(1f, 2f, 3f) + Vector3(10f, 20f, 30f)
    val v4 = Vector4(1f, 2f, 3f, 4f) + Vector4(10f, 20f, 30f, 40f)

    globalAnySink = v3 as Any
    globalAnySink = v4 as Any

    val v3Boxed = globalAnySink as Vector4
    if (v3Boxed.x != 11f) throw Exception("Math failed")
}

interface StackShifter {
    fun executeCall()
}

class Shift0 : StackShifter {
    override fun executeCall() {
        doMathAndForceBoxing()
    }
}
class Shift1 : StackShifter {
    override fun executeCall() {
        val a = 1L; doMathAndForceBoxing(); globalAnySink = a
    }
}
class Shift2 : StackShifter {
    override fun executeCall() {
        val a=1L; val b=2L; doMathAndForceBoxing(); globalAnySink = a+b
    }
}
class Shift3 : StackShifter {
    override fun executeCall() {
        val a=1L; val b=2L; val c=3L; doMathAndForceBoxing(); globalAnySink = a+b+c
    }
}
class Shift4 : StackShifter {
    override fun executeCall() {
        val a=1L; val b=2L; val c=3L; val d=4L; doMathAndForceBoxing(); globalAnySink = a+b+c+d
    }
}

fun box(): String {
    val shifters = arrayOf(Shift0(), Shift1(), Shift2(), Shift3(), Shift4())

    for (shifter in shifters) {
        try {
            shifter.executeCall()
        } catch (e: Exception) {
            return "FAIL: Crash dynamically caught or unexpected exception ${e.message}"
        }
    }

    return "OK"
}