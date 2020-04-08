/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch", "RemoveRedundantCallsOfConversionMethods", "unused", "CanBeParameter")
package perfTestPackage1 // this package is mandatory

import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import kotlinx.cinterop.toCValues
import platform.gles.GL_COLOR_BUFFER_BIT
import platform.gles.GL_CULL_FACE
import platform.gles.GL_CW
import platform.gles.GL_DEPTH_BUFFER_BIT
import platform.gles.GL_DEPTH_TEST
import platform.gles.GL_DIFFUSE
import platform.gles.GL_DITHER
import platform.gles.GL_FASTEST
import platform.gles.GL_FLOAT
import platform.gles.GL_FRONT_AND_BACK
import platform.gles.GL_LIGHT0
import platform.gles.GL_LIGHTING
import platform.gles.GL_MODELVIEW
import platform.gles.GL_NORMAL_ARRAY
import platform.gles.GL_PERSPECTIVE_CORRECTION_HINT
import platform.gles.GL_POSITION
import platform.gles.GL_PROJECTION
import platform.gles.GL_SHININESS
import platform.gles.GL_SMOOTH
import platform.gles.GL_SPECULAR
import platform.gles.GL_TEXTURE_2D
import platform.gles.GL_TEXTURE_COORD_ARRAY
import platform.gles.GL_TRIANGLES
import platform.gles.GL_UNSIGNED_BYTE
import platform.gles.GL_VERTEX_ARRAY
import platform.gles.glClear
import platform.gles.glClearColor
import platform.gles.glDisable
import platform.gles.glDrawElements
import platform.gles.glEnable
import platform.gles.glEnableClientState
import platform.gles.glFrontFace
import platform.gles.glFrustumf
import platform.gles.glHint
import platform.gles.glLightfv
import platform.gles.glLoadIdentity
import platform.gles.glMaterialf
import platform.gles.glMaterialfv
import platform.gles.glMatrixMode
import platform.gles.glMultMatrixf
import platform.gles.glNormalPointer
import platform.gles.glPopMatrix
import platform.gles.glPushMatrix
import platform.gles.glShadeModel
import platform.gles.glTexCoordPointer
import platform.gles.glTranslatef
import platform.gles.glVertexPointer
import platform.gles.glViewport
import platform.posix.sqrtf

private const val WIDTH = 600
private const val HEIGHT = 800
private const val SCALE = 1.25f

fun main() = memScoped {
    glDisable(GL_DITHER)
    glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_FASTEST)
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    glEnable(GL_CULL_FACE)
    glShadeModel(GL_SMOOTH)
    glEnable(GL_DEPTH_TEST)

    glViewport(0, 0, WIDTH, HEIGHT)

    val ratio = WIDTH.toFloat() / HEIGHT
    glMatrixMode(GL_PROJECTION)
    glLoadIdentity()
    glFrustumf(-ratio, ratio, -1.0f, 1.0f, 1.0f, 10.0f)

    glMatrixMode(GL_MODELVIEW)
    glTranslatef(0.0f, 0.0f, -2.0f)
    glLightfv(GL_LIGHT0, GL_POSITION, cValuesOf(1.25f, 1.25f, -2.0f, 0.0f))
    glEnable(GL_LIGHTING)
    glEnable(GL_LIGHT0)
    glEnable(GL_TEXTURE_2D)
    glMaterialfv(GL_FRONT_AND_BACK, GL_DIFFUSE, cValuesOf(0.0f, 1.0f, 1.0f, 1.0f))
    glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR, cValuesOf(0.3f, 0.3f, 0.3f, 1.0f))
    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 30.0f)

    glPushMatrix()
    glMatrixMode(GL_MODELVIEW)


    val matrix = allocArray<FloatVar>(16)
    for (i in 0..3)
        for (j in 0..3)
            matrix[i * 4 + j] = if (i == j) 1.0f else 0.0f

    glMultMatrixf(matrix)

    glClear((GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT).toUInt())

    glEnableClientState(GL_VERTEX_ARRAY)
    glEnableClientState(GL_NORMAL_ARRAY)
    glEnableClientState(GL_TEXTURE_COORD_ARRAY)

    val polygon = RegularPolyhedra.Dodecahedron
    val vertices = mutableListOf<Float>()
    val texCoords = mutableListOf<Float>()
    val triangles = mutableListOf<Byte>()
    val normals = mutableListOf<Float>()

    val texturePoints = arrayOf(Vector2(0.0f, 0.2f), Vector2(0.0f, 0.8f), Vector2(0.6f, 1.0f), Vector2(1.0f, 0.5f), Vector2(0.8f, 0.0f))


    for (face in polygon.faces) {
        val u = polygon.vertices[face[2].toInt()] - polygon.vertices[face[1].toInt()]
        val v = polygon.vertices[face[0].toInt()] - polygon.vertices[face[1].toInt()]
        val normal = u.crossProduct(v).normalized()

        val copiedFace = ByteArray(face.size)
        for (j in face.indices) {
            copiedFace[j] = (vertices.size / 4).toByte()
            polygon.vertices[face[j].toInt()].copyCoordinatesTo(vertices)
            vertices.add(SCALE)
            normal.copyCoordinatesTo(normals)
            texturePoints[j].copyCoordinatesTo(texCoords)
        }

        for (j in 1..face.size - 2) {
            triangles.add(copiedFace[0])
            triangles.add(copiedFace[j])
            triangles.add(copiedFace[j + 1])
        }
    }

    glFrontFace(GL_CW)
    glVertexPointer(4, GL_FLOAT, 0, vertices.toFloatArray().toCValues().ptr)
    glTexCoordPointer(2, GL_FLOAT, 0, texCoords.toFloatArray().toCValues().ptr)
    glNormalPointer(GL_FLOAT, 0, normals.toFloatArray().toCValues().ptr)
    glDrawElements(GL_TRIANGLES, triangles.size, GL_UNSIGNED_BYTE, triangles.toByteArray().toCValues().ptr)

    glPopMatrix()
}

private class Vector2(val x: Float, val y: Float) {
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

private class Vector3(val x: Float, val y: Float, val z: Float) {
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

private const val Zero = 0.0f
private const val DodeA = 0.93417235896f // (Sqrt(5) + 1) / (2 * Sqrt(3))
private const val DodeB = 0.35682208977f // (Sqrt(5) - 1) / (2 * Sqrt(3))
private const val DodeC = 0.57735026919f // 1 / Sqrt(3)
private const val IcosA = 0.52573111212f // Sqrt(5 - Sqrt(5)) / Sqrt(10)
private const val IcosB = 0.85065080835f // Sqrt(5 + Sqrt(5)) / Sqrt(10)

private enum class RegularPolyhedra(val vertices: Array<Vector3>, val faces: Array<ByteArray>) {
    Dodecahedron(
            arrayOf(
                    Vector3(-DodeA, Zero, DodeB), Vector3(-DodeA, Zero, -DodeB), Vector3(DodeA, Zero, -DodeB),
                    Vector3(DodeA, Zero, DodeB), Vector3(DodeB, -DodeA, Zero), Vector3(-DodeB, -DodeA, Zero),
                    Vector3(-DodeB, DodeA, Zero), Vector3(DodeB, DodeA, Zero), Vector3(Zero, DodeB, -DodeA),
                    Vector3(Zero, -DodeB, -DodeA), Vector3(Zero, -DodeB, DodeA), Vector3(Zero, DodeB, DodeA),
                    Vector3(-DodeC, -DodeC, DodeC), Vector3(-DodeC, -DodeC, -DodeC), Vector3(DodeC, -DodeC, -DodeC),
                    Vector3(DodeC, -DodeC, DodeC), Vector3(-DodeC, DodeC, DodeC), Vector3(-DodeC, DodeC, -DodeC),
                    Vector3(DodeC, DodeC, -DodeC), Vector3(DodeC, DodeC, DodeC)
            ),
            arrayOf(
                    byteArrayOf(0, 12, 10, 11, 16), byteArrayOf(1, 17, 8, 9, 13), byteArrayOf(2, 14, 9, 8, 18),
                    byteArrayOf(3, 19, 11, 10, 15), byteArrayOf(4, 14, 2, 3, 15), byteArrayOf(5, 12, 0, 1, 13),
                    byteArrayOf(6, 17, 1, 0, 16), byteArrayOf(7, 19, 3, 2, 18), byteArrayOf(8, 17, 6, 7, 18),
                    byteArrayOf(9, 14, 4, 5, 13), byteArrayOf(10, 12, 5, 4, 15), byteArrayOf(11, 19, 7, 6, 16)
            )),
    Icosahedron(
            arrayOf(
                    Vector3(-IcosA, Zero, IcosB), Vector3(IcosA, Zero, IcosB), Vector3(-IcosA, Zero, -IcosB),
                    Vector3(IcosA, Zero, -IcosB), Vector3(Zero, IcosB, IcosA), Vector3(Zero, IcosB, -IcosA),
                    Vector3(Zero, -IcosB, IcosA), Vector3(Zero, -IcosB, -IcosA), Vector3(IcosB, IcosA, Zero),
                    Vector3(-IcosB, IcosA, Zero), Vector3(IcosB, -IcosA, Zero), Vector3(-IcosB, -IcosA, Zero)
            ),
            arrayOf(
                    byteArrayOf(1, 4, 0), byteArrayOf(4, 9, 0), byteArrayOf(4, 5, 9), byteArrayOf(8, 5, 4),
                    byteArrayOf(1, 8, 4), byteArrayOf(1, 10, 8), byteArrayOf(10, 3, 8), byteArrayOf(8, 3, 5),
                    byteArrayOf(3, 2, 5), byteArrayOf(3, 7, 2), byteArrayOf(3, 10, 7), byteArrayOf(10, 6, 7),
                    byteArrayOf(6, 11, 7), byteArrayOf(6, 0, 11), byteArrayOf(6, 1, 0), byteArrayOf(10, 1, 6),
                    byteArrayOf(11, 0, 9), byteArrayOf(2, 11, 9), byteArrayOf(5, 2, 9), byteArrayOf(11, 2, 7)
            )
    );

    val verticesPerFace get() = faces[0].size
}
