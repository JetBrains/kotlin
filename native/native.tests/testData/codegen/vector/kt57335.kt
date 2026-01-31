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

fun performTest(): String {
    val v3 = Vector3(1f, 2f, 3f) + Vector3(10f, 20f, 30f)
    val v4 = Vector4(1f, 2f, 3f, 4f) + Vector4(10f, 20f, 30f, 40f)

    val sumVecV3 = v3.toString()
    val sumVecV4 = v4.toString()

    if (sumVecV3 != "Vector3(11.0, 22.0, 33.0)") return "FAIL: sumVecV3 is ${sumVecV3}"
    if (sumVecV4 != "Vector4(11.0, 22.0, 33.0, 44.0)") return "FAIL: sumVecV4 is ${sumVecV4}"

    return "OK"
}

fun confuseStackAndRun(depth: Int): String {
    if (depth == 0) return performTest()
    return confuseStackAndRun(depth - 1)
}

fun box(): String {
    for (i in 0..10) {
        val result = confuseStackAndRun(i)
        if (result != "OK") return result
    }
    return "OK"
}