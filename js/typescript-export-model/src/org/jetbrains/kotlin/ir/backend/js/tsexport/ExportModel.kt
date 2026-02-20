/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.ClassId

public sealed class ExportedDeclaration {
    public val attributes: MutableSet<ExportedAttribute> = mutableSetOf()
    public open val isProtected: Boolean
        get() = false
}

public sealed class ExportedAttribute {
    public class DeprecatedAttribute(public val message: String) : ExportedAttribute()
    public object DefaultExport : ExportedAttribute()
}

public data class ExportedModule(
    val name: String,
    val moduleKind: ModuleKind,
    val declarations: List<ExportedDeclaration>
)

public class ExportedNamespace(
    public val name: String,
    public val declarations: List<ExportedDeclaration>,
    public val isPrivate: Boolean = false
) : ExportedDeclaration()

public sealed interface ExportedMember {
    public val name: ExportedMemberName
    public val isMember: Boolean
    public val isStatic: Boolean
}

public sealed interface ExportedMemberName {
    public class Identifier(override val value: String) : ExportedMemberName
    public class SymbolReference(override val value: String) : ExportedMemberName

    public val value: String

    public companion object {
        public fun WellKnownSymbol(value: String): SymbolReference = SymbolReference("Symbol.$value")
    }
}

public data class ExportedFunction(
    override val name: ExportedMemberName,
    val returnType: ExportedType,
    val parameters: List<ExportedParameter>,
    val typeParameters: List<ExportedTypeParameter> = emptyList(),
    override val isMember: Boolean = false,
    override val isStatic: Boolean = false,
    val isAbstract: Boolean = false,
    override val isProtected: Boolean,
) : ExportedDeclaration(), ExportedMember

public data class ExportedConstructor(
    val parameters: List<ExportedParameter>,
    val visibility: ExportedVisibility
) : ExportedDeclaration() {
    override val isProtected: Boolean
        get() = visibility == ExportedVisibility.PROTECTED
}

public data class ExportedConstructSignature(
    val parameters: List<ExportedParameter>,
    val returnType: ExportedType,
    val typeParameters: List<ExportedTypeParameter> = emptyList(),
    override val isProtected: Boolean,
) : ExportedDeclaration()

public data class ExportedField(
    override val name: ExportedMemberName,
    val type: ExportedType,
    val mutable: Boolean = true,
    override val isMember: Boolean = false,
    override val isStatic: Boolean = false,
    val isAbstract: Boolean = false,
    override val isProtected: Boolean = false,
    val isObjectGetter: Boolean = false,
    val isOptional: Boolean = false,
    val isQualified: Boolean = false,
) : ExportedDeclaration(), ExportedMember

public sealed interface ExportedPropertyAccessor : ExportedMember {
    public val type: ExportedType
    public val isAbstract: Boolean
}

public data class ExportedPropertyGetter(
    override val name: ExportedMemberName,
    override val type: ExportedType,
    override val isStatic: Boolean = false,
    override val isAbstract: Boolean = false,
    override val isProtected: Boolean = false,
) : ExportedDeclaration(), ExportedPropertyAccessor {
    override val isMember: Boolean
        get() = true
}

public data class ExportedPropertySetter(
    override val name: ExportedMemberName,
    override val type: ExportedType,
    override val isStatic: Boolean = false,
    override val isAbstract: Boolean = false,
    override val isProtected: Boolean = false,
) : ExportedDeclaration(), ExportedPropertyAccessor {
    override val isMember: Boolean
        get() = true
}

// TODO: Cover all cases with frontend and disable error declarations
public class ErrorDeclaration(public val message: String) : ExportedDeclaration()


public sealed class ExportedClass : ExportedDeclaration() {
    public abstract val name: String
    public abstract val members: List<ExportedDeclaration>
    public abstract val superClasses: List<ExportedType>
    public abstract val superInterfaces: List<ExportedType>
    public abstract val nestedClasses: List<ExportedClass>
    public abstract val originalClassId: ClassId?
    public abstract val isCompanion: Boolean
    public abstract val isExternal: Boolean
}

public data class ExportedRegularClass(
    override val name: String,
    val isInterface: Boolean = false,
    val isAbstract: Boolean = false,
    val requireMetadata: Boolean = !isInterface,
    override val superClasses: List<ExportedType> = emptyList(),
    override val superInterfaces: List<ExportedType> = emptyList(),
    val typeParameters: List<ExportedTypeParameter>,
    override val members: List<ExportedDeclaration>,
    override val nestedClasses: List<ExportedClass>,
    override val originalClassId: ClassId?,
    override val isExternal: Boolean,
) : ExportedClass() {
    override val isCompanion: Boolean
        get() = false
}

public data class ExportedObject(
    override val name: String,
    override val superClasses: List<ExportedType> = emptyList(),
    override val superInterfaces: List<ExportedType> = emptyList(),
    override val members: List<ExportedDeclaration>,
    override val nestedClasses: List<ExportedClass>,
    val typeParameters: List<ExportedTypeParameter> = emptyList(),
    override val originalClassId: ClassId?,
    override val isExternal: Boolean,
    override val isCompanion: Boolean,
    val isTopLevel: Boolean,
) : ExportedClass()

public class ExportedParameter(
    public val name: String?,
    public val type: ExportedType,
    public val hasDefaultValue: Boolean = false
)

public sealed class ExportedType {
    public open fun replaceTypes(substitution: Map<ExportedType, ExportedType>): ExportedType =
        substitution[this] ?: this

    public sealed class Primitive(public val typescript: kotlin.String) : ExportedType() {
        public object Boolean : Primitive("boolean")
        public object Number : Primitive("number")
        public object BigInt : Primitive("bigint")
        public object ByteArray : Primitive("Int8Array")
        public object ShortArray : Primitive("Int16Array")
        public object IntArray : Primitive("Int32Array")
        public object FloatArray : Primitive("Float32Array")
        public object DoubleArray : Primitive("Float64Array")
        public object LongArray : Primitive("BigInt64Array")
        public object String : Primitive("string")
        public object Throwable : Primitive("Error")
        public object Any : Primitive("any")
        public object Undefined : Primitive("undefined")
        public object Unit : Primitive("void")
        public object Nothing : Primitive("never")
        public object UniqueSymbol : Primitive("unique symbol")
        public object Unknown : Primitive("unknown") {
            override fun withNullability(nullable: kotlin.Boolean): ExportedType =
                if (nullable) this else NonNullable(this)
        }
    }

    public sealed class LiteralType<T : Any>(public val value: T) : ExportedType() {
        public class StringLiteralType(value: String) : LiteralType<String>(value)
        public class NumberLiteralType(value: Number) : LiteralType<Number>(value)
        public class BooleanLiteralType(value: Boolean) : LiteralType<Boolean>(value)
    }

    public data class Array(val elementType: ExportedType) : ExportedType() {
        override fun replaceTypes(substitution: Map<ExportedType, ExportedType>): ExportedType =
            substitution[this] ?: Array(elementType.replaceTypes(substitution))
    }

    public class Function(
        public val parameters: List<ExportedParameter>,
        public val returnType: ExportedType
    ) : ExportedType()

    public class ConstructorType(
        public val typeParameters: List<ExportedTypeParameter>,
        public val returnType: ExportedType
    ) : ExportedType()

    public data class ClassType(
        val name: String,
        val arguments: List<ExportedType>,
        val classId: ClassId? = null,
    ) : ExportedType() {
        override fun equals(other: Any?): Boolean = this === other || other is ClassType && classId == other.classId
        override fun hashCode(): Int = classId.hashCode()

        override fun replaceTypes(substitution: Map<ExportedType, ExportedType>): ExportedType =
            substitution[this] ?: copy(arguments = arguments.map { it.replaceTypes(substitution) })
    }

    public data class TypeParameterRef(val typeParameter: ExportedTypeParameter) : ExportedType()
    public class Nullable(public val baseType: ExportedType) : ExportedType()
    public class NonNullable(public val baseType: ExportedType) : ExportedType()
    public class ErrorType(public val comment: String) : ExportedType()
    public data class TypeOf(val classType: ClassType) : ExportedType()
    public class ObjectsParentType(public val constructor: ExportedType) : ExportedType()

    public class InlineInterfaceType(
        public val members: List<ExportedDeclaration>
    ) : ExportedType()

    public class InlineArrayType(public val elements: List<ExportedType>) : ExportedType()

    public class UnionType(public val lhs: ExportedType, public val rhs: ExportedType) : ExportedType()

    public class IntersectionType(public val lhs: ExportedType, public val rhs: ExportedType) : ExportedType()

    public class PropertyType(public val container: ExportedType, public val propertyName: ExportedType) : ExportedType()

    public data class ImplicitlyExportedType(val type: ExportedType, val exportedSupertype: ExportedType) : ExportedType() {
        override fun withNullability(nullable: Boolean): ImplicitlyExportedType =
            ImplicitlyExportedType(type.withNullability(nullable), exportedSupertype.withNullability(nullable))
    }

    public open fun withNullability(nullable: Boolean): ExportedType =
        if (nullable) Nullable(this) else this

    public fun withImplicitlyExported(implicitlyExportedType: Boolean, exportedSupertype: ExportedType): ExportedType =
        if (implicitlyExportedType) ImplicitlyExportedType(this, exportedSupertype) else this
}

public data class ExportedTypeParameter(val name: String, var constraint: ExportedType? = null)

public enum class ExportedVisibility(public val keyword: String) {
    DEFAULT(""),
    PRIVATE("private "),
    PROTECTED("protected ")
}
