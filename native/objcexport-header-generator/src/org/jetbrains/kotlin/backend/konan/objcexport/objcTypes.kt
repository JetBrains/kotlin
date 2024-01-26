/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

sealed class ObjCType {
    final override fun toString(): String = this.render()

    abstract fun render(attrsAndName: String): String

    fun render() = render("")

    protected fun String.withAttrsAndName(attrsAndName: String) =
        if (attrsAndName.isEmpty()) this else "$this ${attrsAndName.trimStart()}"
}

data class ObjCRawType(
    val rawText: String,
) : ObjCType() {
    override fun render(attrsAndName: String): String = rawText.withAttrsAndName(attrsAndName)
}

sealed class ObjCReferenceType : ObjCType() {
    @InternalKotlinNativeApi
    open val classId: ClassId? = null
}

sealed class ObjCNonNullReferenceType : ObjCReferenceType()

data class ObjCNullableReferenceType(
    val nonNullType: ObjCNonNullReferenceType,
    val isNullableResult: Boolean = false,
) : ObjCReferenceType() {

    override val classId: ClassId? get() = nonNullType.classId

    override fun render(attrsAndName: String): String {
        val attribute = if (isNullableResult) objcNullableResultAttribute else objcNullableAttribute
        return nonNullType.render(" $attribute".withAttrsAndName(attrsAndName))
    }
}

data class ObjCClassType(
    val className: String,
    val typeArguments: List<ObjCNonNullReferenceType> = emptyList(),
    override val classId: ClassId? = null,
) : ObjCNonNullReferenceType() {


    override fun render(attrsAndName: String) = buildString {
        append(className)
        if (typeArguments.isNotEmpty()) {
            append("<")
            typeArguments.joinTo(this) { it.render() }
            append(">")
        }
        append(" *")
        append(attrsAndName)
    }
}

sealed class ObjCGenericTypeUsage : ObjCNonNullReferenceType() {
    abstract val typeName: String
    final override fun render(attrsAndName: String): String {
        return typeName.withAttrsAndName(attrsAndName)
    }
}

data class ObjCGenericTypeRawUsage(override val typeName: String) : ObjCGenericTypeUsage()

data class ObjCGenericTypeParameterUsage(
    override val typeName: String,
) : ObjCGenericTypeUsage()

data class ObjCProtocolType(
    val protocolName: String,
    override val classId: ClassId? = null,
) : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String) = "id<$protocolName>".withAttrsAndName(attrsAndName)
}

object ObjCIdType : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String) = "id".withAttrsAndName(attrsAndName)
}

object ObjCInstanceType : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String): String = "instancetype".withAttrsAndName(attrsAndName)
}

data class ObjCBlockPointerType(
    val returnType: ObjCType,
    val parameterTypes: List<ObjCReferenceType>,
) : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String) = returnType.render(buildString {
        append("(^")
        append(attrsAndName)
        append(")(")
        if (parameterTypes.isEmpty()) append("void")
        parameterTypes.joinTo(this) { it.render() }
        append(')')
    })
}

object ObjCMetaClassType : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String): String = "Class".withAttrsAndName(attrsAndName)
}

sealed class ObjCPrimitiveType(
    val cName: String,
) : ObjCType() {
    object NSUInteger : ObjCPrimitiveType("NSUInteger")
    object BOOL : ObjCPrimitiveType("BOOL")
    object unichar : ObjCPrimitiveType("unichar")
    object int8_t : ObjCPrimitiveType("int8_t")
    object int16_t : ObjCPrimitiveType("int16_t")
    object int32_t : ObjCPrimitiveType("int32_t")
    object int64_t : ObjCPrimitiveType("int64_t")
    object uint8_t : ObjCPrimitiveType("uint8_t")
    object uint16_t : ObjCPrimitiveType("uint16_t")
    object uint32_t : ObjCPrimitiveType("uint32_t")
    object uint64_t : ObjCPrimitiveType("uint64_t")
    object float : ObjCPrimitiveType("float")
    object double : ObjCPrimitiveType("double")
    object NSInteger : ObjCPrimitiveType("NSInteger")
    object char : ObjCPrimitiveType("char")
    object unsigned_char : ObjCPrimitiveType("unsigned char")
    object unsigned_short : ObjCPrimitiveType("unsigned short")
    object int : ObjCPrimitiveType("int")
    object unsigned_int : ObjCPrimitiveType("unsigned int")
    object long : ObjCPrimitiveType("long")
    object unsigned_long : ObjCPrimitiveType("unsigned long")
    object long_long : ObjCPrimitiveType("long long")
    object unsigned_long_long : ObjCPrimitiveType("unsigned long long")
    object short : ObjCPrimitiveType("short")

    override fun render(attrsAndName: String) = cName.withAttrsAndName(attrsAndName)
}

data class ObjCPointerType(
    val pointee: ObjCType,
    val nullable: Boolean = false,
) : ObjCType() {
    override fun render(attrsAndName: String) =
        pointee.render(
            "*${
                if (nullable) {
                    " $objcNullableAttribute".withAttrsAndName(attrsAndName)
                } else {
                    attrsAndName
                }
            }"
        )
}

object ObjCVoidType : ObjCType() {
    override fun render(attrsAndName: String) = "void".withAttrsAndName(attrsAndName)
}

@InternalKotlinNativeApi
enum class ObjCValueType(val encoding: String) {
    BOOL("c"),
    UNICHAR("S"),

    // TODO: Switch to explicit SIGNED_CHAR
    CHAR("c"),
    SHORT("s"),
    INT("i"),
    LONG_LONG("q"),
    UNSIGNED_CHAR("C"),
    UNSIGNED_SHORT("S"),
    UNSIGNED_INT("I"),
    UNSIGNED_LONG_LONG("Q"),
    FLOAT("f"),
    DOUBLE("d"),
    POINTER("^v")
}

enum class ObjCVariance(internal val declaration: String) {
    INVARIANT(""),
    COVARIANT("__covariant "),
    CONTRAVARIANT("__contravariant ");

    companion object {
        fun fromKotlinVariance(variance: Variance): ObjCVariance = when (variance) {
            Variance.OUT_VARIANCE -> COVARIANT
            Variance.IN_VARIANCE -> CONTRAVARIANT
            else -> INVARIANT
        }
    }
}

sealed class ObjCGenericTypeDeclaration {
    abstract val typeName: String
    abstract val variance: ObjCVariance
    final override fun toString(): String = variance.declaration + typeName
}

data class ObjCGenericTypeRawDeclaration(
    override val typeName: String,
    override val variance: ObjCVariance = ObjCVariance.INVARIANT,
) : ObjCGenericTypeDeclaration()

data class ObjCGenericTypeParameterDeclaration(
    override val typeName: String,
    override val variance: ObjCVariance,
) : ObjCGenericTypeDeclaration()

@InternalKotlinNativeApi
fun ObjCType.makeNullableIfReferenceOrPointer(): ObjCType = when (this) {
    is ObjCPointerType -> ObjCPointerType(this.pointee, nullable = true)

    is ObjCNonNullReferenceType -> ObjCNullableReferenceType(this)

    is ObjCNullableReferenceType, is ObjCRawType, is ObjCPrimitiveType, ObjCVoidType -> this
}

const val objcNullableAttribute = "_Nullable"
const val objcNullableResultAttribute = "_Nullable_result"