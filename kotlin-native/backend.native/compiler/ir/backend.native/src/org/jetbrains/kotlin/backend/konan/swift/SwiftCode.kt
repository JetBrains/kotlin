/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

import java.util.*

private const val DEFAULT_INDENT = "    "

/**
 * Swift code generation utilities.
 *
 * While this class hierarchy mimics swift AST to a some degree, it doesn't aim to facilitate parsing or
 * go anywhere beyound source-level, like performing type-check or general diagnostics.
 *
 */
sealed interface SwiftCode {
    fun render(): String

    companion object {
        inline fun <R> build(build: Builder.() -> R): R = object : Builder {}.build()
    }

    interface Builder {
        /** Creates a labeled argument (label: expression) for use with call-like syntax */
        infix fun String.of(value: Expression) = Argument.Named(this, value)

        val String.identifier get() = Expression.Identifier(this)

        val String.type get() = Type.Nominal(this)

        val String.literal get() = Expression.StringLiteral(this)

        val Number.literal get() = Expression.NumericLiteral(this)
    }

    interface DeclarationsBuilder : Builder {
        operator fun <T : Declaration> T.unaryPlus(): T
    }

    interface StatementsBuilder : Builder {
        operator fun <T : Statement> T.unaryPlus(): T
    }

    sealed interface Import : SwiftCode {
        data class Module(val name: String) : Import {
            override fun render(): String = "import $name"
        }

        data class Symbol(val type: Type, val module: String, val name: String) : Import {
            enum class Type(val displayName: String) {
                STRUCT("struct"),
                CLASS("class"),
                ENUM("enum"),
                PROTOCOL("protocol"),
                TYPEALIAS("typealias"),
                FUNC("func"),
                LET("let"),
                VAR("VAR"),
            }

            override fun render(): String = "import ${type.displayName} $module.$name"
        }
    }

    sealed interface Declaration : Statement {
        enum class Visibility(private val displayName: String) : SwiftCode {
            PRIVATE("private"),
            FILEPRIVATE("fileprivate"),
            INTERNAL("internal"),
            PUBLIC("public"),
            PACKAGE("package");

            override fun render(): String = displayName

            fun renderAsPrefix(): String = takeIf { it != INTERNAL }?.render()?.let { "$it " } ?: ""
        }

        val attributes: List<Attribute>
        val visibility: Visibility

        data class TypeAlias(
                val name: String,
                val type: Type,
                override val attributes: List<Attribute> = emptyList(),
                override val visibility: Visibility = Visibility.INTERNAL,
        ) : Declaration {
            override fun render(): String {
                return "${visibility.renderAsPrefix()}typealias ${name.escapeIdentifierIfNeeded()} = ${type.render()}"
            }
        }

        data class Function(
                val name: String,
                val parameters: List<Parameter> = emptyList(),
                val returnType: Type? = null,
                val isMutating: Boolean = false,
                val isStatic: Boolean = false,
                val isFinal: Boolean = false,
                val isOverride: Boolean = false,
                val isAsync: Boolean = false,
                val isThrowing: Boolean = false,
                override val attributes: List<Attribute> = emptyList(),
                override val visibility: Visibility = Visibility.INTERNAL,
                val code: CodeBlock?,
        ) : Declaration {
            data class Parameter(
                    val argumentName: String?,
                    val parameterName: String? = null,
                    val type: Type,
                    val isInout: Boolean = false,
                    val isVariadic: Boolean = false,
                    val defaultValue: Expression? = null,
            ) : SwiftCode {
                override fun render(): String {
                    return listOfNotNull(
                            (argumentName ?: "_") + (parameterName?.let { " $it" } ?: "") + ":",
                            "inout".takeIf { isInout },
                            type.render(),
                            "...".takeIf { isVariadic },
                            defaultValue?.let { "= " + it.render() }
                    ).joinToString(separator = " ")
                }
            }

            override fun render(): String {
                val parameters = parameters.joinToString(separator = ", ") { it.render() }.let { "($it)" }
                return listOfNotNull(
                        attributes.render(),
                        visibility.renderAsPrefix(),
                        "mutating ".takeIf { isMutating },
                        "static ".takeIf { isStatic },
                        "final ".takeIf { isFinal },
                        "override ".takeIf { isOverride },
                        "func ",
                        name.escapeIdentifierIfNeeded(),
                        parameters,
                        " async".takeIf { isAsync },
                        " throws".takeIf { isThrowing },
                        returnType?.render()?.let { " -> $it" },
                        code?.renderAsBlock()?.let { " $it" }
                ).joinToString(separator = "")
            }
        }

        sealed interface Variable : Declaration {
            val name: String
            val type: Type?
        }

        data class Constant(
                override val name: String,
                override val type: Type? = null,
                val value: Expression? = null,
                override val attributes: List<Attribute> = emptyList(),
                override val visibility: Visibility = Visibility.INTERNAL,
        ) : Variable {
            override fun render(): String {
                return listOfNotNull(
                        attributes.render(),
                        visibility.renderAsPrefix(),
                        "let ",
                        name,
                        type?.render()?.let { ": $it" },
                        value?.render()?.let { " = $it" },
                ).joinToString(separator = "")
            }
        }

        data class StoredVariable(
                override val name: String,
                override val type: Type? = null,
                val value: Expression? = null,
                override val attributes: List<Attribute> = emptyList(),
                override val visibility: Visibility = Visibility.INTERNAL,
                val willSet: Observer? = null,
                val didSet: Observer? = null,
        ) : Variable {
            data class Observer(val name: String, val argumentName: String? = null, val body: CodeBlock) : SwiftCode {
                override fun render(): String {
                    val arguments = argumentName?.let { "($it)" } ?: ""
                    val body = body.renderAsBlock()
                    return "$name$arguments$body"
                }
            }

            override fun render(): String {
                return listOfNotNull(
                        attributes.render(),
                        visibility.renderAsPrefix(),
                        "var ",
                        name,
                        type?.render()?.let { ": $it" },
                        value?.render()?.let { " = $it" },
                        listOfNotNull(willSet, didSet).takeIf { it.isNotEmpty() }
                                ?.joinToString(separator = "\n") { it.render() }
                                ?.let { " {${it.prependIndent(DEFAULT_INDENT)}\n}" }
                ).joinToString(separator = "")
            }
        }

        data class ComputedVariable(
                override val name: String,
                override val type: Type,
                override val attributes: List<Attribute> = emptyList(),
                override val visibility: Visibility = Visibility.INTERNAL,
                val get: Accessor? = null,
                val set: Accessor? = null,
        ) : Variable {
            data class Accessor(val name: String, val argumentName: String? = null, val body: CodeBlock) : SwiftCode {
                override fun render(): String {
                    val arguments = argumentName?.let { "($it)" } ?: ""
                    val body = body.renderAsBlock()
                    return "$name$arguments$body"
                }
            }

            override fun render(): String {
                return listOfNotNull(
                        attributes.render(),
                        visibility.renderAsPrefix(),
                        "var ",
                        name,
                        type.render().let { ": $it" },
                        listOfNotNull(get, set).takeIf { it.isNotEmpty() }
                                ?.joinToString(separator = "\n") { it.render() }
                                ?.let { " {${it.prependIndent(DEFAULT_INDENT)}\n}" }
                ).joinToString(separator = "")
            }
        }
    }

    sealed interface Pattern : SwiftCode {
        data class Identifier(val name: String) : Pattern {
            override fun render(): String = name.escapeIdentifierIfNeeded()
        }

        data class Optional(val pattern: Pattern) : Pattern {
            override fun render(): String = "${pattern.render()}?"
        }

        data class Tuple(val elements: List<Pattern>) : Pattern {
            override fun render(): String = elements.joinToString { it.render() }.let { "($it)" }
        }

        data class Expression(val expression: SwiftCode.Expression) : Pattern {
            override fun render(): String = expression.render()
        }

        data class EnumCase(val type: Type?, val name: String, val elements: Tuple?) : Pattern {
            override fun render(): String {
                return listOfNotNull(
                        type?.render(),
                        name.escapeIdentifierIfNeeded(),
                        elements?.render(),
                ).joinToString(separator = "")
            }
        }

        data object Wildcard : Pattern {
            override fun render(): String = "_"
        }

        data class Binding(val pattern: Pattern, val isMutable: Boolean = false) : Pattern {
            override fun render(): String = (if (isMutable) "var" else "let") + pattern.render()
        }

        data class TypeCast(val pattern: Pattern, val type: Type) : Pattern {
            override fun render(): String = "${pattern.render()} as ${type.render()}"
        }

        data class TypeCheck(val type: Type) : Pattern {
            override fun render(): String = "is ${type.render()}"
        }
    }

    sealed interface Condition : SwiftCode {
        data class OptionalBinding(val name: String, val isMutable: Boolean = false, val value: Expression) : Condition {
            override fun render(): String {
                return (if (isMutable) "var" else "let") + " ${name.escapeIdentifierIfNeeded()} = ${value.render()}"
            }
        }

        data class TupleBinding(val names: List<String>, val isMutable: Boolean = false, val value: Expression) : Condition {
            override fun render(): String {
                val names = names.joinToString(separator = ", ").let { "($it)" }
                return (if (isMutable) "var" else "let") + " $names = ${value.render()}"
            }
        }

        data class PatternMatching(val pattern: Pattern, val value: Expression) : Condition {
            override fun render(): String {
                return "case ${pattern.render()} = ${value.render()}"
            }
        }
    }

    sealed interface Statement : SwiftCode {
        data class Assign(val receiver: Expression, val value: Expression) : Statement {
            override fun render(): String {
                val receiver = receiver.render()
                val value = value.render()
                return "$receiver = $value"
            }
        }

        data class For(val pattern: Pattern, val collection: Expression, val where: Expression?, val body: CodeBlock) : Statement {
            override fun render(): String {
                val pattern = (if (pattern is Pattern.Identifier) "" else "case ") + pattern.render()
                val where = where?.let { " where ${it.render()}" }
                return "for $pattern in ${collection.render()}$where ${body.renderAsBlock()}"
            }
        }

        data class If(val conditions: List<Condition>, val body: CodeBlock) : Statement {
            override fun render(): String {
                val conditions = conditions.takeIf { it.isNotEmpty() }?.joinToString(separator = ", ") { it.render() } ?: "true"
                val body = body.renderAsBlock()
                return if (conditions.isMultiline())
                    "if\n${conditions.prependIndent(DEFAULT_INDENT)}\n$body"
                else
                    "if $conditions $body"
            }
        }

        data class Else(val target: If, val conditions: List<Condition>? = null, val body: CodeBlock) : Statement {
            override fun render(): String {
                val target = target.render()
                return if (conditions != null)
                    "$target else ${If(conditions, body).render()}"
                else
                    "$target else ${body.renderAsBlock()}"
            }
        }

        data class While(val conditions: List<Condition>, val body: CodeBlock) : Statement {
            override fun render(): String {
                val conditions = conditions.takeIf { it.isNotEmpty() }?.joinToString(separator = ", ") { it.render() } ?: "true"
                val body = body.renderAsBlock()
                return if (conditions.isMultiline())
                    "while\n${conditions.prependIndent(DEFAULT_INDENT)}\n$body"
                else
                    "while $conditions $body"
            }
        }

        data class Repeat(val conditions: List<Expression>, val body: CodeBlock) : Statement {
            override fun render(): String {
                val conditions = conditions.takeIf { it.isNotEmpty() }?.joinToString(separator = ", ") { it.render() } ?: "true"
                val body = body.renderAsBlock()
                return "repeat $body while $conditions"
            }
        }

        data class Return(val value: Expression) : Statement {
            override fun render(): String = "return ${value.render()}"
        }

        data object Continue : Statement {
            override fun render(): String = "continue"
        }

        data object Break : Statement {
            override fun render(): String = "break"
        }

        data class Defer(val body: CodeBlock) : Statement {
            override fun render(): String = "defer " + body.renderAsBlock()
        }
    }

    sealed interface Argument : SwiftCode {
        data class Named(val name: String, val value: Expression) : Argument {
            override fun render(): String = "$name: ${value.render()}"
        }
    }

    sealed interface Expression : Statement, Argument, Condition {
        data class Identifier(val name: String) : Expression {
            override fun render(): String = name
        }

        data class Access(val receiver: Expression?, val name: String) : Expression {
            override fun render(): String {
                val receiver = receiver?.render() ?: ""
                val name = name.escapeIdentifierIfNeeded()
                return "$receiver.$name"
            }
        }

        data class Call(val receiver: Expression, val arguments: List<Argument> = emptyList()) : Expression {
            override fun render(): String {
                val receiver = receiver.render()
                val arguments = arguments.joinToString { it.render() }
                return "$receiver($arguments)"
            }
        }

        data class Subscript(val receiver: Expression, val arguments: List<Argument> = emptyList()) : Expression {
            override fun render(): String {
                val receiver = receiver.render()
                val arguments = arguments.joinToString { it.render() }
                return "$receiver[$arguments]"
            }
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

        data class ArrayLiteral(val value: List<Expression>) : Expression {
            override fun render(): String = value.joinToString(separator = ", ") {
                it.render()
            }.let { "[$it]" }
        }

        data class DictionaryLiteral(val value: Map<Expression, Expression>) : Expression {
            override fun render(): String = value.entries.joinToString(separator = ", ") {
                "${it.key.render()}: ${it.value.render()}"
            }.let { "[$it]" }
        }

        object Nil : Expression {
            override fun render(): String = "nil"
        }
    }

    sealed interface Type : SwiftCode {
        data class Nominal(val name: String) : Type {
            override fun render(): String = name
        }

        data class Optional(val type: Type, val isImplicitlyUnwrapped: Boolean = false) : Type {
            override fun render(): String {
                return type.render().parenthesizeIfNeccessary() + if (isImplicitlyUnwrapped) "!" else "?"
            }
        }

        data class Array(val type: Type) : Type {
            override fun render(): String = "[${type.render()}]"
        }

        data class Dictionary(val keyType: Type, val valueType: Type) : Type {
            override fun render(): String {
                val keyType = keyType.render()
                val valueType = valueType.render()
                return "[$keyType: $valueType]"
            }
        }

        data class Tuple(val types: List<Type> = emptyList()) : Type {
            override fun render(): String = types.joinToString(separator = ", ") { it.render() }.let { "($it)" }
        }

        data class Function(val arguments: List<Type> = emptyList(), val result: Type?) : Type {
            override fun render(): String {
                val arguments = arguments.joinToString(separator = ", ") { it.render() }
                val result = (result ?: Nominal("Swift.Void")).render()
                return "($arguments) -> $result"
            }
        }

        data class Metatype(val type: Type, val isExistential: Boolean = false) : Type {
            override fun render(): String {
                return type.render().parenthesizeIfNeccessary() + if (isExistential) ".Protocol" else ".Type"
            }
        }

        data class Opaque(val type: Type) : Type {
            override fun render(): String = "some ${type.render().parenthesizeIfNeccessary()}"
        }

        data class Existential(val protocols: List<String> = emptyList()) : Type {
            override fun render(): String = protocols.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = " & ")
                    ?.let { "any $it" } ?: "Any"
        }
    }

    class File : SwiftCode {
        class Builder(
                var imports: MutableList<Import> = mutableListOf(),
                var declarations: MutableList<Declaration> = mutableListOf()
        ) : DeclarationsBuilder {
            fun Import.add() {
                this@Builder.imports.add(this)
            }

            override fun <T : Declaration> T.unaryPlus(): T = also { this@Builder.declarations.add(it) }
        }

        private val imports: List<Import>
        private val declarations: List<Declaration>

        constructor(imports: List<Import>, declarations: List<Declaration>) {
            this.declarations = declarations
            this.imports = imports
        }

        constructor(block: Builder.() -> Unit) {
            val builder = Builder().apply(block)
            this.declarations = builder.declarations.toList()
            this.imports = builder.imports.toList()
        }

        override fun render() = renderLines().joinToString(separator = "\n")

        fun renderLines(): Sequence<String> = sequence {
            imports.forEach { yield(it.render() + "\n") }
            yield("\n")
            declarations.forEach { yield(it.render() + "\n\n") }
        }
    }

    class CodeBlock(val statements: List<Statement>) : SwiftCode {
        class Builder(
                var statements: MutableList<Statement> = mutableListOf()
        ) : StatementsBuilder, DeclarationsBuilder {
            override fun <T : Declaration> T.unaryPlus(): T = also { this@Builder.statements.add(this) }

            override fun <T : Statement> T.unaryPlus() = also { this@Builder.statements.add(it) }
        }

        constructor(block: Builder.() -> Unit) : this(Builder().apply(block).statements.toList())

        override fun render(): String = statements.joinToString(separator = "\n") { it.render() }

        fun renderAsBlock() = render().prependIndent(DEFAULT_INDENT).let { "{\n$it\n}" }
    }

    data class Attribute(val name: String, val arguments: List<Argument>? = null) : SwiftCode {
        override fun render(): String {
            return "@" + Expression.Identifier(name).render() + (arguments?.joinToString { it.render() }?.let { "($it)" } ?: "")
        }
    }
}

private fun String.escapeIdentifierIfNeeded(): String {
    return this
}

private fun String.parenthesizeIfNeccessary(): String {
    // this is NOT "correct parentheses sequence"
    val needsEscaping = run {
        val stack = Stack<Char>()
        val map = mapOf('(' to ')', '[' to ']', '{' to '}', '<' to '>')
        for (it in this) when {
            it.isWhitespace() -> if (stack.isEmpty()) return@run true
            stack.isNotEmpty() && stack.peek() == it -> stack.pop()
            else -> map[it]?.let { stack.push(it) }
        }
        stack.isEmpty()
    }

    return if (needsEscaping) "($this)" else this
}

private fun String.isMultiline() = any { it == '\n' }

private fun List<SwiftCode.Attribute>.render(separator: String = "\n", terminator: String = "\n"): String? {
    return takeIf { it.isNotEmpty() }?.joinToString(separator = separator, postfix = terminator) { it.render() }
}

fun SwiftCode.File.Builder.import(name: String) = SwiftCode.Import.Module(name)

fun SwiftCode.File.Builder.import(type: SwiftCode.Import.Symbol.Type, module: String, name: String) = SwiftCode.Import.Symbol(type, module, name)

//region Declarations

fun SwiftCode.Builder.attribute(name: String, vararg arguments: SwiftCode.Argument): SwiftCode.Attribute {
    return SwiftCode.Attribute(name, arguments.takeIf { it.isNotEmpty() }?.toList())
}

fun SwiftCode.Builder.`typealias`(name: String, type: SwiftCode.Type) = SwiftCode.Declaration.TypeAlias(name, type)

fun SwiftCode.Builder.function(
        name: String,
        parameters: List<SwiftCode.Declaration.Function.Parameter> = emptyList(),
        type: SwiftCode.Type? = null,
        isMutating: Boolean = false,
        isStatic: Boolean = false,
        isFinal: Boolean = false,
        isOverride: Boolean = false,
        isAsync: Boolean = false,
        isThrowing: Boolean = false,
        attributes: List<SwiftCode.Attribute> = emptyList(),
        visibility: SwiftCode.Declaration.Visibility = SwiftCode.Declaration.Visibility.INTERNAL,
        body: (SwiftCode.StatementsBuilder.() -> Unit)? = null
): SwiftCode.Declaration.Function {
    return SwiftCode.Declaration.Function(
            name,
            parameters,
            type,
            isMutating,
            isStatic,
            isFinal,
            isOverride,
            isAsync,
            isThrowing,
            attributes,
            visibility,
            body?.let { SwiftCode.CodeBlock(it) }
    )
}

fun SwiftCode.Builder.parameter(
        argumentName: String?,
        parameterName: String? = null,
        type: SwiftCode.Type,
        isInout: Boolean = false,
        isVariadic: Boolean = false,
        defaultValue: SwiftCode.Expression? = null,
) = SwiftCode.Declaration.Function.Parameter(argumentName, parameterName, type, isInout, isVariadic, defaultValue)

fun SwiftCode.Builder.`var`(
        name: String,
        type: SwiftCode.Type? = null,
        value: SwiftCode.Expression? = null,
        attributes: List<SwiftCode.Attribute> = emptyList(),
        visibility: SwiftCode.Declaration.Visibility = SwiftCode.Declaration.Visibility.INTERNAL,
        willSet: SwiftCode.Declaration.StoredVariable.Observer? = null,
        didSet: SwiftCode.Declaration.StoredVariable.Observer? = null,
): SwiftCode.Declaration.StoredVariable {
    return SwiftCode.Declaration.StoredVariable(name, type, value, attributes, visibility, willSet = willSet, didSet = didSet)
}

fun SwiftCode.Builder.`var`(
        name: String,
        type: SwiftCode.Type,
        attributes: List<SwiftCode.Attribute> = emptyList(),
        visibility: SwiftCode.Declaration.Visibility = SwiftCode.Declaration.Visibility.INTERNAL,
        get: SwiftCode.Declaration.ComputedVariable.Accessor? = null,
        set: SwiftCode.Declaration.ComputedVariable.Accessor? = null,
): SwiftCode.Declaration.ComputedVariable {
    return SwiftCode.Declaration.ComputedVariable(name, type, attributes, visibility, get = get, set = set)
}

fun SwiftCode.Builder.let(
        name: String,
        type: SwiftCode.Type? = null,
        value: SwiftCode.Expression? = null,
        attributes: List<SwiftCode.Attribute> = emptyList(),
        visibility: SwiftCode.Declaration.Visibility = SwiftCode.Declaration.Visibility.INTERNAL,
): SwiftCode.Declaration.Constant {
    return SwiftCode.Declaration.Constant(name, type, value, attributes, visibility)
}

fun SwiftCode.Builder.willSet(
        arg: String? = null,
        body: SwiftCode.CodeBlock.Builder.() -> Unit
) = SwiftCode.Declaration.StoredVariable.Observer("willSet", argumentName = arg, SwiftCode.CodeBlock(body))

fun SwiftCode.Builder.didSet(
        arg: String? = null,
        body: SwiftCode.CodeBlock.Builder.() -> Unit
) = SwiftCode.Declaration.StoredVariable.Observer("didSet", argumentName = arg, SwiftCode.CodeBlock(body))

fun SwiftCode.Builder.get(
        body: SwiftCode.CodeBlock.Builder.() -> Unit
) = SwiftCode.Declaration.ComputedVariable.Accessor("get", argumentName = null, SwiftCode.CodeBlock(body))

fun SwiftCode.Builder.set(
        arg: String? = null,
        body: SwiftCode.CodeBlock.Builder.() -> Unit
) = SwiftCode.Declaration.ComputedVariable.Accessor("set", argumentName = arg, SwiftCode.CodeBlock(body))

//endregion

//region Statements

fun SwiftCode.Expression.assign(expression: SwiftCode.Expression) = SwiftCode.Statement.Assign(this, expression)

fun SwiftCode.Builder.`return`(expression: SwiftCode.Expression) = SwiftCode.Statement.Return(expression)

val SwiftCode.Builder.`break` get() = SwiftCode.Statement.Break

val SwiftCode.Builder.`continue` get() = SwiftCode.Statement.Continue

fun SwiftCode.Builder.defer(body: SwiftCode.StatementsBuilder.() -> Unit) = SwiftCode.Statement.Defer(SwiftCode.CodeBlock(body))

//endregion

//region Expressions

val SwiftCode.Builder.nil: SwiftCode.Expression.Nil get() = SwiftCode.Expression.Nil

fun SwiftCode.Builder.access(name: String) = SwiftCode.Expression.Access(null, name)

fun SwiftCode.Expression.access(name: String) = SwiftCode.Expression.Access(this, name)

fun SwiftCode.Expression.call(vararg arguments: SwiftCode.Argument) = SwiftCode.Expression.Call(this, arguments.toList())

fun SwiftCode.Expression.call(arguments: List<SwiftCode.Argument>) = SwiftCode.Expression.Call(this, arguments.toList())

fun SwiftCode.Expression.subscript(vararg arguments: SwiftCode.Argument) = SwiftCode.Expression.Subscript(this, arguments.toList())

//endregion

//region Types

val SwiftCode.Builder.any: SwiftCode.Type.Nominal get() = "Any".type

val SwiftCode.Builder.anyObject: SwiftCode.Type.Nominal get() = "AnyObject".type

val SwiftCode.Builder.self: SwiftCode.Type.Nominal get() = "Self".type

val SwiftCode.Type.metatype: SwiftCode.Type.Metatype get() = SwiftCode.Type.Metatype(this)

val SwiftCode.Type.existentialMetatype: SwiftCode.Type.Metatype get() = SwiftCode.Type.Metatype(this, isExistential = true)

val SwiftCode.Type.opaque: SwiftCode.Type.Opaque get() = SwiftCode.Type.Opaque(this)

val SwiftCode.Type.optional: SwiftCode.Type.Optional get() = SwiftCode.Type.Optional(this)

fun SwiftCode.Builder.function(vararg argumentTypes: SwiftCode.Type, returnType: SwiftCode.Type) = SwiftCode.Type.Function(argumentTypes.toList(), returnType)

fun SwiftCode.Builder.tuple(vararg types: SwiftCode.Type) = SwiftCode.Type.Tuple(types.toList())

fun SwiftCode.Builder.array(type: SwiftCode.Type) = SwiftCode.Type.Array(type)

fun SwiftCode.Builder.dictionary(key: SwiftCode.Type, value: SwiftCode.Type) = SwiftCode.Type.Dictionary(key, value)

fun SwiftCode.Builder.dictionary(types: Pair<SwiftCode.Type, SwiftCode.Type>) = SwiftCode.Type.Dictionary(types.first, types.second)

fun SwiftCode.Builder.existential(vararg types: String) = SwiftCode.Type.Existential(types.toList())

//endregion