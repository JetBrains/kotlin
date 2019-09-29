/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.androidnative

import kotlinx.cinterop.*
import platform.android.*
import platform.posix.*

class Vector2(val x: Float, val y: Float) {
    val length by lazy { sqrtf(x * x + y * y) }

    fun normalized(): Vector2 {
        val len = length
        return Vector2(x / len, y / len)
    }

    fun copyCoordinatesTo(arr: MutableList<Float>) {
        arr.add(x)
        arr.add(y)
    }

    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun times(other: Float) = Vector2(x * other, y * other)
    operator fun div(other: Float) = Vector2(x / other, y / other)

    companion object {
        val Zero = Vector2(0.0f, 0.0f)
    }
}

class Vector3(val x: Float, val y: Float, val z: Float) {
    val length by lazy { sqrtf(x * x + y * y + z * z) }

    fun crossProduct(other: Vector3): Vector3 =
            Vector3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    fun normalized(): Vector3 {
        val len = length
        return Vector3(x / len, y / len, z / len)
    }

    fun copyCoordinatesTo(arr: MutableList<Float>) {
        arr.add(x)
        arr.add(y)
        arr.add(z)
    }

    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
}

