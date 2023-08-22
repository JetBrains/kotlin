/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

/**
 * C code generation utilities.
 *
 * While this class hierarchy mimics swift AST to a some degree, it doesn't aim to facilitate parsing or
 * go anywhere beyound source-level, like performing type-check or general diagnostics.
 *
 */
sealed interface CCode {
    fun render(): String

    companion object {
        inline fun <R> build(build: Builder.() -> R): R = (object : Builder {}).build()
    }

    interface Builder {
        val String.identifier get() = Expression.Identifier(this)

        val String.type get() = Type.Typedef(this)

        val String.literal get() = Expression.StringLiteral(this)

        val Number.literal get() = Expression.NumericLiteral(this)
    }

    interface FileScopeBuilder : Builder {
        operator fun <T : CCode> T.unaryPlus(): T
    }

    data class Include(val path: String, val isLocal: Boolean = false) : CCode {
        override fun render(): String = if (isLocal) "#include \"$path\"" else "#include <$path>"
    }

    data class Pragma(val command: String) : CCode {
        override fun render(): String = "#pragma $command"
    }

    sealed interface Type : CCode {
        val precedence: Int get() = 0

        fun renderDeclaration(name: String? = null): String
        override fun render(): String = renderDeclaration(null)

        fun Type.parenthesizeIfNecessary(name: String?) = name?.let {
            if (this.precedence > this@parenthesizeIfNecessary.precedence) "($name)" else name
        } ?: ""

        data object Void : Type {
            override fun renderDeclaration(name: String?): String = "void" + name.indented
        }

        data object Size : Type {
            override fun renderDeclaration(name: String?): String = "size_t" + name.indented
        }

        data object PointerDifference : Type {
            override fun renderDeclaration(name: String?): String = "ptrdiff_t" + name.indented
        }

        data object NullPtr : Type {
            override fun renderDeclaration(name: String?): String = "nullptr_t" + name.indented
        }

        data object MaximumAlignment : Type {
            override fun renderDeclaration(name: String?): String = "max_align_t" + name.indented
        }

        sealed interface Primitive : Type

        abstract class Modifier(val type: Primitive? = null, private val displayName: String) : Primitive {
            override fun renderDeclaration(name: String?): String = displayName + (type?.renderDeclaration(name) ?: name).indented
        }

        class Short(type: Primitive? = null) : Modifier(type, "short")

        class Long(type: Primitive? = null) : Modifier(type, "long")

        class Signed(type: Primitive? = null) : Modifier(type, "signed")

        class Unsigned(type: Primitive? = null) : Modifier(type, "unsigned")

        enum class Numeric(private val displayName: String) : Primitive {
            BOOL("_Bool"),
            CHAR("char"),
            INT("int"),
            FLOAT("float"),
            DOUBLE("double");

            override fun renderDeclaration(name: String?): String = displayName + name.indented
        }

        data class FixedWidthInteger(val size: Size, val isUnsigned: Boolean) : Type {
            enum class Size(val displayName: String) {
                EXACTLY8("8"),
                EXACTLY16("16"),
                EXACTLY32("32"),
                EXACTLY64("64"),
                POINTER("ptr"),
                MAX("max")
                // Omitted: least- and fast- types.
            }

            override fun renderDeclaration(name: String?): String = "${if (isUnsigned) "u" else ""}int${size.displayName}_t" + name.indented
        }

        data class Const(val type: Type) : Type {
            override fun renderDeclaration(name: String?): String = type.renderDeclaration("const" + name.indented)
        }

        data class Volatile(val type: Type) : Type {
            override fun renderDeclaration(name: String?): String = type.renderDeclaration("volatile" + name.indented)
        }

        data class Atomic(val type: Type) : Type {
            override fun renderDeclaration(name: String?): String = type.renderDeclaration("_Atomic" + name.indented)
        }

        data class Struct(val name: String) : Type {
            override fun renderDeclaration(name: String?): String = "struct ${this.name}" + name.indented
        }

        data class Union(val name: String) : Type {
            override fun renderDeclaration(name: String?): String = "union ${this.name}" + name.indented
        }

        data class Enum(val name: String) : Type {
            override fun renderDeclaration(name: String?): String = "enum ${this.name}" + name.indented
        }

        data class Typedef(val name: String) : Type {
            override fun renderDeclaration(name: String?): String = this.name + name.indented
        }

        data class Pointer(val type: Type, val isRestrict: Boolean = false, val nullability: Nullability? = null) : Type {
            enum class Nullability(private val displayName: String) : CCode {
                NULLABLE("_Nullable"),
                NONNULL("_Nonnull"),
                EXPLICITLY_UNSPECIFIED("_Null_unspecified"),
                NULLABLE_RESULT("_Nullable_result");

                override fun render(): String = displayName
            }

            override fun renderDeclaration(name: String?): String {
                val signature = listOfNotNull(
                        "restrict".takeIf { isRestrict },
                        nullability?.render(),
                        name,
                ).joinToString(separator = " ", postfix = " ")
                return type.renderDeclaration("*$signature")
            }

            override val precedence get() = 3
        }

        data class Array(val type: Type, val size: Expression?) : Type {
            override fun renderDeclaration(name: String?): String = type.renderDeclaration(
                    type.parenthesizeIfNecessary(name) + "[${size?.render() ?: ""}]"
            )

            override val precedence get() = 5
        }

        data class Function(val returnType: Type = Void, val argumentTypes: List<Declarator> = emptyList()) : Type {
            override fun renderDeclaration(name: String?): String {
                val arguments = "(${argumentTypes.joinToString { it.render() }})"
                return returnType.renderDeclaration((name ?: "") + arguments)
            }

            override val precedence get() = 4
        }

        data class Attributed(val returnType: Type, val attributes: List<Attribute>) : Type {
            override fun renderDeclaration(name: String?): String = attributes
                    .joinToString { it.render() }
                    .let { "$it " } + returnType.renderDeclaration(name)
        }
    }

    sealed interface Expression : CCode {
        data class Identifier(val name: String) : Expression {
            override fun render(): String = name
        }

        data class StringLiteral(val value: String) : Expression {
            override fun render(): String = value
                    .replace("\"", "\\\"")
                    .replace("\\", "\\\\")
                    .let { "\"$it\"" }
        }

        data class NumericLiteral(val value: Number) : Expression {
            override fun render(): String = value.toString()
        }

        data object NULL : Expression {
            override fun render(): String = "NULL"
        }

        data class Call(val receiver: Expression, val arguments: List<Expression>) : Expression {
            override fun render(): String = receiver.render() + arguments.joinToString { it.render() }.let { "($it)" }
        }
    }

    data class Declarator(val type: Type, val name: String? = null, val attributes: List<Attribute> = emptyList()) : CCode {
        override fun render(): String = type.renderDeclaration(name) + attributes.joinToString(prefix = " ") { it.render() }
    }

    data class Declaration(
            val declarators: List<Declarator>,
            val specifiersAndQualifiers: List<SpecifierOrQualifier> = emptyList(),
            val attributes: List<Attribute> = emptyList(),
    ) : CCode {
        sealed interface SpecifierOrQualifier : CCode

        enum class StorageQualifier(private val displayName: String) : SpecifierOrQualifier {
            TYPEDEF("typedef"),
            CONSTEXPR("constexpr"),
            AUTO("auto"),
            // Obsolete: REGISTER
            STATIC("static"),
            EXTERN("extern"),
            THREAD_LOCAL("_Thread_local");

            override fun render(): String = displayName
        }

        enum class FunctionQualifier(private val displayName: String) : SpecifierOrQualifier {
            INLINE("inline"),
            NORETURN("_Noreturn");

            override fun render(): String = displayName
        }

        data class AlignAsSpecifier(val alignment: CCode) : SpecifierOrQualifier {
            override fun render(): String = "_Alignas(${alignment.render()})"
        }

        override fun render(): String {
            val attributes = attributes.joinToString(separator = " ", postfix = "\n") { it.render() }
            val snq = specifiersAndQualifiers.joinToString(separator = " ", postfix = " ") { it.render() }
            val declarators = declarators.joinToString { it.render() }
            return "$attributes$snq$declarators;"
        }
    }

    sealed interface Attribute : CCode {
        data class GNU(val arguments: List<Expression> = emptyList()) : Attribute {
            override fun render(): String = "__attribute__((${arguments.joinToString { it.render() }}))"
        }

        data class Raw(val expresion: Expression) : Attribute {
            override fun render(): String = expresion.render()
        }

        data class CSTD(val prefix: String? = null, val identifier: String, val arguments: List<Expression> = emptyList()) : Attribute {
            override fun render(): String = listOfNotNull(
                    prefix?.let { "$it::" },
                    identifier,
                    arguments.takeIf { it.isNotEmpty() }?.joinToString { it.render() }?.let { "($it)" }
            ).joinToString(prefix = "[[", separator = "", postfix = "]]")
        }
    }

    class File : CCode {
        class Builder(var items: MutableList<CCode> = mutableListOf()) : FileScopeBuilder {
            override fun <T : CCode> T.unaryPlus(): T = also { this@Builder.items.add(it) }
        }

        private val items: List<CCode>

        constructor(items: List<CCode>) {
            this.items = items
        }

        constructor(block: Builder.() -> Unit) {
            this.items = Builder().apply(block).items
        }

        override fun render(): String = renderLines().joinToString(separator = "\n")

        fun renderLines(): Sequence<String> = sequence {
            items.forEach { yield(it.render() + "\n") }
        }
    }
}

private val String?.indented get() = this?.let { " $it" } ?: ""

//region Preprocessor

fun CCode.Builder.include(path: String, isLocal: Boolean = false) = CCode.Include(path, isLocal)

fun CCode.Builder.pragma(command: String) = CCode.Pragma(command)

//endregion

//region Types

val CCode.Builder.void get() = CCode.Type.Void

val CCode.Builder.size get() = CCode.Type.Size

val CCode.Builder.ptrdiff get() = CCode.Type.PointerDifference

val CCode.Builder.nullptr get() = CCode.Type.NullPtr

val CCode.Builder.max_align get() = CCode.Type.MaximumAlignment

val CCode.Builder.bool get() = CCode.Type.Numeric.BOOL

val CCode.Builder.char get() = CCode.Type.Numeric.CHAR

val CCode.Builder.int get() = CCode.Type.Numeric.INT

val CCode.Builder.float get() = CCode.Type.Numeric.FLOAT

val CCode.Builder.double get() = CCode.Type.Numeric.DOUBLE

val CCode.Builder.short get() = CCode.Type.Short(null)

val CCode.Builder.long get() = CCode.Type.Long(null)

val CCode.Builder.signed get() = CCode.Type.Signed(null)

val CCode.Builder.unsigned get() = CCode.Type.Unsigned(null)

val CCode.Type.Primitive.short get() = CCode.Type.Short(this)

val CCode.Type.Primitive.long get() = CCode.Type.Long(this)

val CCode.Type.Primitive.signed get() = CCode.Type.Signed(this)

val CCode.Type.Primitive.unsigned get() = CCode.Type.Unsigned(this)

fun CCode.Builder.int8(isUnsigned: Boolean = false) = CCode.Type.FixedWidthInteger(CCode.Type.FixedWidthInteger.Size.EXACTLY8, isUnsigned)
fun CCode.Builder.int16(isUnsigned: Boolean = false) = CCode.Type.FixedWidthInteger(CCode.Type.FixedWidthInteger.Size.EXACTLY16, isUnsigned)
fun CCode.Builder.int32(isUnsigned: Boolean = false) = CCode.Type.FixedWidthInteger(CCode.Type.FixedWidthInteger.Size.EXACTLY32, isUnsigned)
fun CCode.Builder.int64(isUnsigned: Boolean = false) = CCode.Type.FixedWidthInteger(CCode.Type.FixedWidthInteger.Size.EXACTLY64, isUnsigned)

fun CCode.Builder.intptr(isUnsigned: Boolean = false) = CCode.Type.FixedWidthInteger(CCode.Type.FixedWidthInteger.Size.POINTER, isUnsigned)
fun CCode.Builder.intmax(isUnsigned: Boolean = false) = CCode.Type.FixedWidthInteger(CCode.Type.FixedWidthInteger.Size.MAX, isUnsigned)

val CCode.Type.pointer get() = CCode.Type.Pointer(this)

fun CCode.Type.pointer(
        isRestrict: Boolean = false,
        nullability: CCode.Type.Pointer.Nullability? = null
) = CCode.Type.Pointer(this, isRestrict, nullability)

val CCode.Type.const get() = CCode.Type.Const(this)

val CCode.Type.volatile get() = CCode.Type.Volatile(this)

fun CCode.Type.array(size: CCode.Expression? = null) = CCode.Type.Array(this, size)

fun CCode.Builder.functionType(returnType: CCode.Type = CCode.Type.Void) = CCode.Type.Function(returnType)

fun CCode.Type.function(vararg parameters: CCode.Type) = CCode.Type.Function(this, parameters.map { CCode.Declarator(it) })

fun CCode.Type.function(parameters: List<CCode.Declarator>) = CCode.Type.Function(this, parameters)

//endregion

//region Declarations

val CCode.Builder.typedef get() = CCode.Declaration.StorageQualifier.TYPEDEF

val CCode.Builder.constexpr get() = CCode.Declaration.StorageQualifier.CONSTEXPR

val CCode.Builder.auto get() = CCode.Declaration.StorageQualifier.AUTO

val CCode.Builder.static get() = CCode.Declaration.StorageQualifier.STATIC

val CCode.Builder.extern get() = CCode.Declaration.StorageQualifier.EXTERN

val CCode.Builder.thread_local get() = CCode.Declaration.StorageQualifier.THREAD_LOCAL

val CCode.Builder.inline get() = CCode.Declaration.FunctionQualifier.INLINE

val CCode.Builder.noreturn get() = CCode.Declaration.FunctionQualifier.NORETURN

fun CCode.Builder.gnuAttribute(vararg arguments: CCode.Expression) = CCode.Attribute.GNU(arguments.toList())

fun CCode.Builder.gnuAttribute(arguments: List<CCode.Expression>) = CCode.Attribute.GNU(arguments.toList())

fun CCode.Builder.rawAttribute(expresion: CCode.Expression) = CCode.Attribute.Raw(expresion)

fun CCode.Builder.stdAttribute(
        prefix: String? = null,
        identifier: String,
        vararg arguments: CCode.Expression
) = CCode.Attribute.CSTD(prefix, identifier, arguments.toList())

fun CCode.Builder.stdAttribute(
        prefix: String? = null,
        identifier: String,
        arguments: List<CCode.Expression> = emptyList()
) = CCode.Attribute.CSTD(prefix, identifier, arguments.toList())

fun CCode.Builder.declare(
        declarators: List<CCode.Declarator>,
        qualifiers: List<CCode.Declaration.SpecifierOrQualifier> = emptyList(),
        attributes: List<CCode.Attribute> = emptyList()
) = CCode.Declaration(declarators, qualifiers, attributes)

fun CCode.Builder.declare(
        vararg declarators: CCode.Declarator,
        qualifiers: List<CCode.Declaration.SpecifierOrQualifier> = emptyList(),
        attributes: List<CCode.Attribute> = emptyList()
) = CCode.Declaration(declarators.toList(), qualifiers, attributes)

fun CCode.Builder.function(
        returnType: CCode.Type = CCode.Type.Void,
        name: String,
        vararg arguments: CCode.Declarator,
        attributes: List<CCode.Attribute> = emptyList()
) = CCode.Declarator(CCode.Type.Function(returnType, arguments.toList()), name, attributes)

fun CCode.Builder.function(
        returnType: CCode.Type = CCode.Type.Void,
        name: String,
        arguments: List<CCode.Declarator>,
        attributes: List<CCode.Attribute> = emptyList()
) = CCode.Declarator(CCode.Type.Function(returnType, arguments), name, attributes)

fun CCode.Builder.variable(
        type: CCode.Type,
        name: String? = null,
        attributes: List<CCode.Attribute> = emptyList()
) = CCode.Declarator(type, name, attributes)

//endregion

//region Expressions

val CCode.Builder.`null` get() = CCode.Expression.NULL

fun CCode.Expression.call(vararg arguments: CCode.Expression) = CCode.Expression.Call(this, arguments.toList())

fun CCode.Expression.call(arguments: List<CCode.Expression>) = CCode.Expression.Call(this, arguments.toList())

//endregion