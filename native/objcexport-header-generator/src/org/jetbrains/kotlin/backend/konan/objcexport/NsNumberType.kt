/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.name.ClassId

/*
Defining ClassIds for Unsigned types:
Cannot reuse 'UnsignedType.kt' from core/descriptors
 */
private val uByteClassId = ClassId.fromString("kotlin/UByte")
private val uShortClassId = ClassId.fromString("kotlin/UShort")
private val uIntClassId = ClassId.fromString("kotlin/UInt")
private val uLongClassId = ClassId.fromString("kotlin/ULong")

@InternalKotlinNativeApi
enum class NSNumberKind(val mappedKotlinClassId: ClassId?, val objCType: ObjCType) {
    CHAR(PrimitiveType.BYTE, ObjCPrimitiveType.char),
    UNSIGNED_CHAR(uByteClassId, ObjCPrimitiveType.unsigned_char),
    SHORT(PrimitiveType.SHORT, ObjCPrimitiveType.short),
    UNSIGNED_SHORT(uShortClassId, ObjCPrimitiveType.unsigned_short),
    INT(PrimitiveType.INT, ObjCPrimitiveType.int),
    UNSIGNED_INT(uIntClassId, ObjCPrimitiveType.unsigned_int),
    LONG(ObjCPrimitiveType.long),
    UNSIGNED_LONG(ObjCPrimitiveType.unsigned_long),
    LONG_LONG(PrimitiveType.LONG, ObjCPrimitiveType.long_long),
    UNSIGNED_LONG_LONG(uLongClassId, ObjCPrimitiveType.unsigned_long_long),
    FLOAT(PrimitiveType.FLOAT, ObjCPrimitiveType.float),
    DOUBLE(PrimitiveType.DOUBLE, ObjCPrimitiveType.double),
    BOOL(PrimitiveType.BOOLEAN, ObjCPrimitiveType.BOOL),
    INTEGER(ObjCPrimitiveType.NSInteger),
    UNSIGNED_INTEGER(ObjCPrimitiveType.NSUInteger)

    ;

    // UNSIGNED_SHORT -> unsignedShort
    private val kindName = this.name.split('_')
        .joinToString("") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }.replaceFirstChar(Char::lowercaseChar)

    val valueSelector = kindName // unsignedShort
    val initSelector = "initWith${kindName.replaceFirstChar(Char::uppercaseChar)}:" // initWithUnsignedShort:
    val factorySelector = "numberWith${kindName.replaceFirstChar(Char::uppercaseChar)}:" // numberWithUnsignedShort:

    constructor(
        primitiveType: PrimitiveType,
        objCPrimitiveType: ObjCPrimitiveType,
    ) : this(ClassId.topLevel(primitiveType.typeFqName), objCPrimitiveType)

    constructor(objCPrimitiveType: ObjCPrimitiveType) : this(null, objCPrimitiveType)
}


