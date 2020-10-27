/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.androidnative

const val Zero = 0.0f
const val DodeA = 0.93417235896f // (Sqrt(5) + 1) / (2 * Sqrt(3))
const val DodeB = 0.35682208977f // (Sqrt(5) - 1) / (2 * Sqrt(3))
const val DodeC = 0.57735026919f // 1 / Sqrt(3)
const val IcosA = 0.52573111212f // Sqrt(5 - Sqrt(5)) / Sqrt(10)
const val IcosB = 0.85065080835f // Sqrt(5 + Sqrt(5)) / Sqrt(10)

enum class RegularPolyhedra(val vertices: Array<Vector3>, val faces: Array<ByteArray>) {
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