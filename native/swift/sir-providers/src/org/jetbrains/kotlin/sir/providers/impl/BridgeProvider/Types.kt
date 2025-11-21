/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.BridgeProvider

internal sealed class CType {
    abstract fun render(name: String): String

    val nullable: CType
        get() = NullabilityAnnotated(unwrapAnnotated(), Nullability.NULLABLE)
    val nonnulll: CType
        get() = NullabilityAnnotated(unwrapAnnotated(), Nullability.NONNULL)

    fun unwrapAnnotated(): CType = (this as? NullabilityAnnotated)?.wrapped ?: this

    sealed class Predefined(private val repr: String) : CType() {
        override fun render(name: String): String = if (name.isBlank()) repr else "$repr $name"
    }

    enum class Nullability(val keyword: String) {
        NULLABLE("_Nullable"),
        NONNULL("_Nonnull"),
        NULL_UNSPECIFIED("_Null_unspecified"),
    }

    class NullabilityAnnotated(val wrapped: CType, val nullability: Nullability) : CType() {
        override fun render(name: String): String = wrapped.render(nullability.keyword + " " + (name.takeIf { name.isNotBlank() } ?: ""))
    }

    data object Void : Predefined("void")
    data object Bool : Predefined("_Bool")
    data object Int8 : Predefined("int8_t")
    data object Int16 : Predefined("int16_t")
    data object Int32 : Predefined("int32_t")
    data object Int64 : Predefined("int64_t")
    data object UInt8 : Predefined("uint8_t")
    data object UInt16 : Predefined("uint16_t")
    data object UInt32 : Predefined("uint32_t")
    data object UInt64 : Predefined("uint64_t")
    data object Float : Predefined("float")
    data object Double : Predefined("double")
    data object Object : Predefined("void *")
    data object OutObject : Predefined("void *_Nullable *")
    data object id : Predefined("id")
    data object NSString : Predefined("NSString *")
    data object NSNumber : Predefined("NSNumber *")
    data object NSObject : Predefined("id<NSObject>") // NSProxy and NSObject conforms to this

    sealed class Generic(base: String, vararg args: CType) : Predefined(
        repr = "$base<${args.joinToString(", ") { it.render("").trim() }}> *"
    )

    class NSArray(elem: CType) : Generic("NSArray", elem)
    class NSSet(elem: CType) : Generic("NSSet", elem)
    class NSDictionary(key: CType, value: CType) : Generic("NSDictionary", key, value)

    class BlockPointer(val parameters: List<CType>, val returnType: CType) : CType() {
        override fun render(name: String): String = returnType.render(buildString {
            append("(")
            append("^$name")
            append(")(")
            append(parameters.printCParametersForBlock())
            append(')')
        })

        private fun List<CType>.printCParametersForBlock(): String = if (isEmpty()) {
            "void" // A block declaration without a prototype is deprecated
        } else {
            joinToString { it.render("") }
        }
    }
}

internal enum class KotlinType(val representation: String, val isPrimitiveNumber: Boolean = false) {
    Unit("Unit"),

    Boolean("Boolean"),
    Char("Char", isPrimitiveNumber = true),

    Byte("Byte", isPrimitiveNumber = true),
    Short("Short", isPrimitiveNumber = true),
    Int("Int", isPrimitiveNumber = true),
    Long("Long", isPrimitiveNumber = true),

    UByte("UByte", isPrimitiveNumber = true),
    UShort("UShort", isPrimitiveNumber = true),
    UInt("UInt", isPrimitiveNumber = true),
    ULong("ULong", isPrimitiveNumber = true),

    Float("Float", isPrimitiveNumber = true),
    Double("Double", isPrimitiveNumber = true),

    KotlinObject("kotlin.native.internal.NativePtr"),

    PointerToKotlinObject("kotlinx.cinterop.COpaquePointerVar"),

    // id, +0
    ObjCObjectUnretained("kotlin.native.internal.NativePtr"),

    String("String"),
    ;

    override fun toString(): String = representation

    fun decapitalized(): String = representation.replaceFirstChar(kotlin.Char::lowercase)
}

internal val KotlinType.defaultValue: String
    get() = when (this) {
        KotlinType.Unit -> "Unit"
        KotlinType.Boolean -> "false"
        KotlinType.Char -> "'\\u0000'"
        KotlinType.Byte,
        KotlinType.Short,
        KotlinType.Int,
        KotlinType.Long,
        KotlinType.UByte,
        KotlinType.UShort,
        KotlinType.UInt,
        KotlinType.ULong,
            -> "0"
        KotlinType.Float,
        KotlinType.Double,
            -> "0.0"
        KotlinType.PointerToKotlinObject -> error("PointerToKotlinObject shouldn't appear in return type position")
        KotlinType.KotlinObject,
        KotlinType.ObjCObjectUnretained, // This is semantically +0, so we're allowed to simply dismiss the pointer.
            -> "kotlin.native.internal.NativePtr.NULL"
        KotlinType.String -> ""
    }