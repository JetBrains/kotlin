/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

sealed class BinaryType<out T> {
    class Primitive(val type: PrimitiveBinaryType) : BinaryType<Nothing>()
    class Reference<T>(val types: Sequence<T>, val nullable: Boolean) : BinaryType<T>()
}

fun BinaryType<*>.primitiveBinaryTypeOrNull(): PrimitiveBinaryType? = when (this) {
    is BinaryType.Primitive -> this.type
    is BinaryType.Reference -> null
}

enum class PrimitiveBinaryType {
    BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, POINTER, VECTOR128
}
