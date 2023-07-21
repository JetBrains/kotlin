/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

import java.util.*

private const val DEFAULT_INDENT = "    "

sealed interface Swift {
    fun render(parent: Swift? = null): String

    companion object {
        inline fun <R> new(build: Builder.() -> R): R = object : Builder {}.build()
    }

    interface Builder {
        infix fun String.of(value: Expression) = Argument.Named(this, value)

        val String.variable: Expression.Variable
            get() = Expression.Variable(this)

        val String.type: Type.Nominal
            get() = Type.Nominal(this)

        val String.literal: Expression.StringLiteral
            get() = Expression.StringLiteral(this)

        val Number.literal: Expression.NumericLiteral
            get() = Expression.NumericLiteral(this)
    }

    interface DeclarationsBuilder : Builder {
        fun <T : Declaration> T.build(): T
    }

    interface StatementsBuilder : Builder {
        fun <T : Statement> T.build(): T
    }

    sealed interface Import : Swift {
        data class Module(val name: String) : Import {
            override fun render(parent: Swift?): String = "import $name"
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

            override fun render(parent: Swift?): String = "import ${type.displayName} $module.$name"
        }
    }

    sealed interface Declaration : Statement {
        enum class Visibility(private val displayName: String) : Swift {
            PRIVATE("private"),
            FILEPRIVATE("fileprivate"),
            INTERNAL("internal"),
            PUBLIC("public"),
            PACKAGE("package");

            override fun render(parent: Swift?): String = displayName

            fun renderAsPrefix(parent: Swift?): String = takeIf { it != INTERNAL }?.render(parent)?.let { "$it " } ?: ""
        }

        val attributes: List<Attribute>
        val visibility: Visibility

        data class TypeAlias(
                val name: String,
                val type: Type,
                override val attributes: List<Attribute> = emptyList(),
                override val visibility: Visibility = Visibility.INTERNAL,
        ) : Declaration {
            override fun render(parent: Swift?): String {
                return "${visibility.renderAsPrefix(this)}typealias ${name.escapeIdentifierIfNeeded()} = ${type.render(this).trim()}"
            }
        }

        data class Function(
                val name: String,
                val parameters: List<Parameter> = emptyList(),
                val type: Type? = null,
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
            ) : Swift {
                override fun render(parent: Swift?): String {
                    return listOfNotNull(
                            (argumentName ?: "_") + (parameterName?.let { " $it" } ?: "") + ":",
                            "inout".takeIf { isInout },
                            type.render(parent).trim(),
                            "...".takeIf { isVariadic },
                            defaultValue?.let { "= " + it.render(parent).trim() }
                    ).joinToString(separator = " ")
                }
            }

            override fun render(parent: Swift?): String {
                val parameters = parameters.joinToString(separator = ", ") { it.render(this).trim() }.let { "($it)" }
                return listOfNotNull(
                        attributes.render(this),
                        visibility.renderAsPrefix(this),
                        "mutating ".takeIf { isMutating },
                        "static ".takeIf { isStatic },
                        "final ".takeIf { isFinal },
                        "override ".takeIf { isOverride },
                        "func ",
                        name.escapeIdentifierIfNeeded(),
                        parameters,
                        " async".takeIf { isAsync },
                        " throws".takeIf { isThrowing },
                        type?.render(this)?.trim()?.let { " -> $it" },
                        code?.renderAsBlock(this)?.let { " $it" }
                ).joinToString(separator = "")
            }
        }

        sealed interface Variable : Declaration {
            val name: String
            val type: Type?
        }

        data class StoredVariable(
                override val name: String,
                override val type: Type? = null,
                val value: Expression? = null,
                override val attributes: List<Attribute> = emptyList(),
                override val visibility: Visibility = Visibility.INTERNAL,
                val observers: List<Observer> = emptyList(),
        ) : Variable {
            data class Observer(val name: String, val argumentName: String? = null, val body: CodeBlock) : Swift {
                override fun render(parent: Swift?): String {
                    val arguments = argumentName?.let { "($it)" } ?: ""
                    val body = body.renderAsBlock(parent)
                    return "$name$arguments$body"
                }
            }

            class Builder(val observers: MutableList<Observer> = mutableListOf()) {
                fun willSet(arg: String? = null, body: CodeBlock.Builder.() -> Unit): Observer {
                    return Observer("willSet", argumentName = arg, CodeBlock(body)).also { observers.add(it) }
                }

                fun didSet(arg: String? = null, body: CodeBlock.Builder.() -> Unit): Observer {
                    return Observer("didSet", argumentName = arg, CodeBlock(body)).also { observers.add(it) }
                }
            }

            constructor(
                    name: String,
                    type: Type? = null,
                    value: Expression? = null,
                    attributes: List<Attribute> = emptyList(),
                    visibility: Visibility = Visibility.INTERNAL,
                    body: Builder.() -> Unit = {},
            ) : this(name, type, value, attributes, visibility, Builder().apply(body).observers.toList())

            override fun render(parent: Swift?): String {
                return listOfNotNull(
                        attributes.render(this),
                        visibility.renderAsPrefix(this),
                        "var ",
                        name,
                        type?.render(this)?.trim()?.let { ": $it" },
                        value?.render(this)?.trim()?.let { " = $it" },
                        observers.takeIf { it.isNotEmpty() }
                                ?.joinToString(separator = "\n") { it.render(this).trim() }
                                ?.let { " {${it.prependIndent(DEFAULT_INDENT)}\n}" }
                ).joinToString(separator = "")
            }
        }

        data class Constant(
                override val name: String,
                override val type: Type? = null,
                val value: Expression? = null,
                override val attributes: List<Attribute> = emptyList(),
                override val visibility: Visibility = Visibility.INTERNAL,
        ) : Variable {
            override fun render(parent: Swift?): String {
                return listOfNotNull(
                        attributes.render(this),
                        visibility.renderAsPrefix(this),
                        "let ",
                        name,
                        type?.render(this)?.trim()?.let { ": $it" },
                        value?.render(this)?.trim()?.let { " = $it" },
                ).joinToString(separator = "")
            }
        }

        data class ComputedVariable(
                override val name: String,
                override val type: Type,
                override val attributes: List<Attribute> = emptyList(),
                override val visibility: Visibility = Visibility.INTERNAL,
                val accessors: List<Accessor> = emptyList(),
        ) : Variable {
            data class Accessor(val name: String, val argumentName: String? = null, val body: CodeBlock) : Swift {
                override fun render(parent: Swift?): String {
                    val arguments = argumentName?.let { "($it)" } ?: ""
                    val body = body.renderAsBlock(parent)
                    return "$name$arguments$body"
                }
            }

            class Builder(val observers: MutableList<Accessor> = mutableListOf()) {
                fun get(body: CodeBlock.Builder.() -> Unit): Accessor {
                    return Accessor("get", argumentName = null, CodeBlock(body)).also { observers.add(it) }
                }

                fun set(arg: String? = null, body: CodeBlock.Builder.() -> Unit): Accessor {
                    return Accessor("set", argumentName = arg, CodeBlock(body)).also { observers.add(it) }
                }
            }

            constructor(
                    name: String,
                    type: Type,
                    attributes: List<Attribute> = emptyList(),
                    visibility: Visibility = Visibility.INTERNAL,
                    body: Builder.() -> Unit = {},
            ) : this(name, type, attributes, visibility, Builder().apply(body).observers.toList())

            override fun render(parent: Swift?): String {
                return listOfNotNull(
                        attributes.render(this),
                        visibility.renderAsPrefix(this),
                        "var ",
                        name,
                        type.render(this).trim().let { ": $it" },
                        accessors.takeIf { it.isNotEmpty() }
                                ?.joinToString(separator = "\n") { it.render(this).trim() }
                                ?.let { " {${it.prependIndent(DEFAULT_INDENT)}\n}" }
                ).joinToString(separator = "")
            }
        }
    }

    sealed interface Pattern : Swift {
        data class Identifier(val name: String) : Pattern {
            override fun render(parent: Swift?): String = name.escapeIdentifierIfNeeded()
        }

        data class Optional(val pattern: Pattern) : Pattern {
            override fun render(parent: Swift?): String = "${pattern.render(parent)}?"
        }

        data class Tuple(val elements: List<Pattern>) : Pattern {
            override fun render(parent: Swift?): String = elements.joinToString { it.render(parent).trim() }.let { "($it)" }
        }

        data class Expression(val expression: Swift.Expression) : Pattern {
            override fun render(parent: Swift?): String = expression.render(this).trim()
        }

        data class EnumCase(val type: Type?, val name: String, val elements: Tuple?) : Pattern {
            override fun render(parent: Swift?): String {
                return listOfNotNull(
                        type?.render(parent),
                        name.escapeIdentifierIfNeeded(),
                        elements?.render(parent),
                ).joinToString(separator = "")
            }
        }

        data object Wildcard : Pattern {
            override fun render(parent: Swift?): String = "_"
        }

        data class Binding(val pattern: Pattern, val isMutable: Boolean = false) : Pattern {
            override fun render(parent: Swift?): String = (if (isMutable) "var" else "let") + pattern.render(parent).trim()
        }

        data class TypeCast(val pattern: Pattern, val type: Type) : Pattern {
            override fun render(parent: Swift?): String = "${pattern.render(parent).trim()} as ${type.render(parent).trim()}"
        }

        data class TypeCheck(val type: Type) : Pattern {
            override fun render(parent: Swift?): String = "is ${type.render(parent).trim()}"
        }
    }

    sealed interface Condition : Swift {
        data class OptionalBinding(val name: String, val isMutable: Boolean = false, val value: Expression) : Condition {
            override fun render(parent: Swift?): String {
                return (if (isMutable) "var" else "let") + " ${name.escapeIdentifierIfNeeded()} = ${value.render(parent).trim()}"
            }
        }

        data class TupleBinding(val names: List<String>, val isMutable: Boolean = false, val value: Expression) : Condition {
            override fun render(parent: Swift?): String {
                val names = names.joinToString(separator = ", ").let { "($it)" }
                return (if (isMutable) "var" else "let") + " $names = ${value.render(parent).trim()}"
            }
        }

        data class PatternMatching(val pattern: Pattern, val value: Expression) : Condition {
            override fun render(parent: Swift?): String {
                return "case ${pattern.render(parent).trim()} = ${value.render(parent).trim()}"
            }
        }
    }

    sealed interface Statement : Swift {
        data class Assign(val receiver: Expression, val value: Expression) : Statement {
            override fun render(parent: Swift?): String {
                val receiver = receiver.render(parent).trim()
                val value = value.render(parent).trim()
                return "$receiver = $value"
            }
        }

        data class For(val pattern: Pattern, val collection: Expression, val where: Expression?, val body: CodeBlock) : Statement {
            override fun render(parent: Swift?): String {
                val pattern = (if (pattern is Pattern.Identifier) "" else "case ") + pattern.render(this)
                val where = where?.let { " where ${it.render(this)}" }
                return "for $pattern in ${collection.render(this)}$where ${body.renderAsBlock(parent)}"
            }
        }

        data class If(val conditions: List<Condition>, val body: CodeBlock) : Statement {
            override fun render(parent: Swift?): String {
                val conditions = conditions.takeIf { it.isNotEmpty() }?.joinToString(separator = ", ") { it.render(this).trim() } ?: "true"
                val body = body.renderAsBlock(parent)
                return if (conditions.isMultiline())
                    "if\n${conditions.prependIndent(DEFAULT_INDENT)}\n$body"
                else
                    "if $conditions $body"
            }
        }

        data class Else(val target: If, val conditions: List<Condition>? = null, val body: CodeBlock) : Statement {
            override fun render(parent: Swift?): String {
                val target = target.render(parent)
                return if (conditions != null)
                    "$target else ${If(conditions, body).render(parent)}"
                else
                    "$target else ${body.renderAsBlock(parent)}"
            }
        }

        data class While(val conditions: List<Condition>, val body: CodeBlock) : Statement {
            override fun render(parent: Swift?): String {
                val conditions = conditions.takeIf { it.isNotEmpty() }?.joinToString(separator = ", ") { it.render(this).trim() } ?: "true"
                val body = body.renderAsBlock(parent)
                return if (conditions.isMultiline())
                    "while\n${conditions.prependIndent(DEFAULT_INDENT)}\n$body"
                else
                    "while $conditions $body"
            }
        }

        data class Repeat(val conditions: List<Expression>, val body: CodeBlock) : Statement {
            override fun render(parent: Swift?): String {
                val conditions = conditions.takeIf { it.isNotEmpty() }?.joinToString(separator = ", ") { it.render(this).trim() } ?: "true"
                val body = body.renderAsBlock(parent)
                return "repeat $body while $conditions"
            }
        }

        data class Return(val value: Expression) : Statement {
            override fun render(parent: Swift?): String = "return ${value.render(this).trim()}"
        }

        data object Continue : Statement {
            override fun render(parent: Swift?): String = "continue"
        }

        data object Break : Statement {
            override fun render(parent: Swift?): String = "break"
        }
    }

    sealed interface Argument : Swift {
        data class Named(val name: String? = null, val value: Expression) : Argument {
            override fun render(parent: Swift?): String {
                val value = value.render(parent).trim()
                return if (name != null) "$name: $value" else value
            }
        }
    }

    sealed interface Expression : Statement, Argument, Condition {
        data class Variable(val name: String) : Expression {
            override fun render(parent: Swift?): String = name
        }

        data class Access(val receiver: Expression?, val name: String) : Expression {
            override fun render(parent: Swift?): String {
                val receiver = receiver?.render(parent)?.trim() ?: ""
                val name = name.escapeIdentifierIfNeeded()
                return "$receiver.$name"
            }
        }

        data class Call(val receiver: Expression, val arguments: List<Argument> = emptyList()) : Expression {
            override fun render(parent: Swift?): String {
                val receiver = receiver.render(parent).trim()
                val arguments = arguments.joinToString { it.render(parent) }
                return "$receiver($arguments)"
            }
        }

        data class Subscript(val receiver: Expression, val arguments: List<Argument> = emptyList()) : Expression {
            override fun render(parent: Swift?): String {
                val receiver = receiver.render(parent).trim()
                val arguments = arguments.joinToString { it.render(parent) }
                return "$receiver[$arguments]"
            }
        }

        data class StringLiteral(val value: String) : Expression {
            override fun render(parent: Swift?): String = value
                    .replace("\"", "\\\"")
                    .replace("\\", "\\\\")
                    .let { "\"$it\"" }
        }

        data class NumericLiteral(val value: Number) : Expression {
            override fun render(parent: Swift?): String = value.toString()
        }

        data class ArrayLiteral(val value: List<Expression>) : Expression {
            override fun render(parent: Swift?): String = value.joinToString(separator = ", ") {
                it.render(parent).trim()
            }.let { "[$it]" }
        }

        data class DictionaryLiteral(val value: Map<Expression, Expression>) : Expression {
            override fun render(parent: Swift?): String = value.entries.joinToString(separator = ", ") {
                "${it.key.render(parent).trim()}: ${it.value.render(parent).trim()}"
            }.let { "[$it]" }
        }

        object Nil : Expression {
            override fun render(parent: Swift?): String = "nil"
        }
    }

    sealed interface Type : Swift {
        data class Nominal(val name: String) : Type {
            override fun render(parent: Swift?): String = name
        }

        data class Optional(val type: Type, val isImplicitlyUnwrapped: Boolean = false) : Type {
            override fun render(parent: Swift?): String {
                return type.render(parent).parenthesizeIfNeccessary() + if (isImplicitlyUnwrapped) "!" else "!"
            }
        }

        data class Array(val type: Type) : Type {
            override fun render(parent: Swift?): String = "[${type.render(parent).trim()}]"
        }

        data class Dictionary(val keyType: Type, val valueType: Type) : Type {
            override fun render(parent: Swift?): String {
                val keyType = keyType.render(parent).trim()
                val valueType = valueType.render(parent).trim()
                return "[$keyType: $valueType]"
            }
        }

        data class Tuple(val types: List<Type> = emptyList()) : Type {
            override fun render(parent: Swift?): String = types.joinToString(separator = ", ") { it.render(parent).trim() }.let { "($it)" }
        }

        data class Function(val arguments: List<Type> = emptyList(), val result: Type?) : Type {
            override fun render(parent: Swift?): String {
                val arguments = arguments.joinToString(separator = ", ") { it.render(parent).trim() }
                val result = (result ?: Nominal("Swift.Void")).render(parent).trim()
                return "($arguments) -> $result"
            }
        }

        data class Metatype(val type: Type, val isExistential: Boolean = false) : Type {
            override fun render(parent: Swift?): String {
                return type.render(parent).parenthesizeIfNeccessary() + if (isExistential) ".Protocol" else ".Type"
            }
        }

        data class Opaque(val type: Type) : Type {
            override fun render(parent: Swift?): String = "some ${type.render(parent).parenthesizeIfNeccessary()}"
        }

        data class Existential(val protocols: List<String> = emptyList()) : Type {
            override fun render(parent: Swift?): String = protocols.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = " & ")
                    ?.let { "any $it" } ?: "Any"
        }
    }

    class File : Swift {
        class Builder(
                var imports: MutableList<Import> = mutableListOf(),
                var declarations: MutableList<Declaration> = mutableListOf()
        ) : DeclarationsBuilder {
            fun Import.add() {
                this@Builder.imports.add(this)
            }

            override fun <T : Declaration> T.build(): T = also { this@Builder.declarations.add(it) }
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

        override fun render(parent: Swift?): String {
            val imports = imports.joinToString(separator = "\n", postfix = "\n\n") { it.render(this).trim() }
            val declarations = declarations.joinToString(separator = "\n\n") { it.render(this).trim() }
            return imports + declarations
        }
    }

    class CodeBlock : Swift {
        class Builder(
                var statements: MutableList<Statement> = mutableListOf()
        ) : StatementsBuilder, DeclarationsBuilder {
            override fun <T : Declaration> T.build(): T = also { this@Builder.statements.add(this) }

            override fun <T : Statement> T.build() = also { this@Builder.statements.add(it) }
        }

        private val statements: List<Statement>

        constructor(statements: List<Statement>) {
            this.statements = statements.toList()
        }

        constructor(block: Builder.() -> Unit) {
            this.statements = Builder().apply(block).statements.toList()
        }

        override fun render(parent: Swift?): String = statements.joinToString(separator = "\n") { it.render(parent).trim() }

        fun renderAsBlock(parent: Swift?) = render(parent).trim().prependIndent(DEFAULT_INDENT).let { "{\n$it\n}" }
    }

    data class Attribute(val name: String, val arguments: List<Argument>? = null) : Swift {
        override fun render(parent: Swift?): String {
            return "@" + Expression.Variable(name).render(parent) + (arguments?.joinToString { it.render(parent) }?.let { "($it)" } ?: "")
        }
    }
}

private fun String.escapeIdentifierIfNeeded(): String {
    return this
}

private fun String.parenthesizeIfNeccessary(): String {
    val self = this.trim()

    // this is NOT "correct parentheses sequence"
    val needsEscaping = run {
        val stack = Stack<Char>()
        val map = mapOf('(' to ')', '[' to ']', '{' to '}', '<' to '>')
        for (it in self) when {
            it.isWhitespace() -> if (stack.isEmpty()) return@run true
            stack.isNotEmpty() && stack.peek() == it -> stack.pop()
            else -> map[it]?.let { stack.push(it) }
        }
        stack.isEmpty()
    }

    return if (needsEscaping) "($self)" else self
}

private fun String.isMultiline() = any { it == '\n' }

private fun List<Swift.Attribute>.render(parent: Swift?, separator: String = "\n", terminator: String = "\n"): String? {
    return takeIf { it.isNotEmpty() }?.joinToString(separator = separator, postfix = terminator) { it.render(parent).trim() }
}

inline fun <T> MutableList<Swift.Declaration>.add(body: Swift.DeclarationsBuilder.() -> T): T {
    val builder = (object : Swift.DeclarationsBuilder {
        val declarations: MutableList<Swift.Declaration> = mutableListOf()
        override fun <T : Swift.Declaration> T.build(): T = also { declarations.add(it) }
    })
    val result = builder.body()
    addAll(builder.declarations)
    return result
}

fun Swift.File.Builder.import(name: String) = Swift.Import.Module(name)

fun Swift.File.Builder.import(type: Swift.Import.Symbol.Type, module: String, name: String) = Swift.Import.Symbol(type, module, name)

//region Declarations

fun Swift.DeclarationsBuilder.attribute(name: String, vararg arguments: Swift.Argument): Swift.Attribute {
    return Swift.Attribute(name, arguments.takeIf { it.isNotEmpty() }?.toList())
}

fun Swift.DeclarationsBuilder.`typealias`(name: String, type: Swift.Type) = Swift.Declaration.TypeAlias(name, type)

fun Swift.DeclarationsBuilder.function(
        name: String,
        parameters: List<Swift.Declaration.Function.Parameter> = emptyList(),
        type: Swift.Type? = null,
        isMutating: Boolean = false,
        isStatic: Boolean = false,
        isFinal: Boolean = false,
        isOverride: Boolean = false,
        isAsync: Boolean = false,
        isThrowing: Boolean = false,
        attributes: List<Swift.Attribute> = emptyList(),
        visibility: Swift.Declaration.Visibility = Swift.Declaration.Visibility.INTERNAL,
        body: (Swift.StatementsBuilder.() -> Unit)? = null
): Swift.Declaration.Function {
    return Swift.Declaration.Function(
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
            body?.let { Swift.CodeBlock(it) }
    )
}

fun Swift.DeclarationsBuilder.parameter(
        argumentName: String?,
        parameterName: String? = null,
        type: Swift.Type,
        isInout: Boolean = false,
        isVariadic: Boolean = false,
        defaultValue: Swift.Expression? = null,
) = Swift.Declaration.Function.Parameter(argumentName, parameterName, type, isInout, isVariadic, defaultValue)

fun Swift.DeclarationsBuilder.`var`(
        name: String,
        type: Swift.Type? = null,
        value: Swift.Expression? = null,
        attributes: List<Swift.Attribute> = emptyList(),
        visibility: Swift.Declaration.Visibility = Swift.Declaration.Visibility.INTERNAL,
        body: Swift.Declaration.StoredVariable.Builder.() -> Unit = {}
): Swift.Declaration.StoredVariable {
    return Swift.Declaration.StoredVariable(name, type, value, attributes, visibility, body)
}

fun Swift.DeclarationsBuilder.`var`(
        name: String,
        type: Swift.Type,
        attributes: List<Swift.Attribute> = emptyList(),
        visibility: Swift.Declaration.Visibility = Swift.Declaration.Visibility.INTERNAL,
        body: Swift.Declaration.ComputedVariable.Builder.() -> Unit = {}
): Swift.Declaration.ComputedVariable {
    return Swift.Declaration.ComputedVariable(name, type, attributes, visibility, body)
}

fun Swift.DeclarationsBuilder.let(
        name: String,
        type: Swift.Type? = null,
        value: Swift.Expression? = null,
        attributes: List<Swift.Attribute> = emptyList(),
        visibility: Swift.Declaration.Visibility = Swift.Declaration.Visibility.INTERNAL,
): Swift.Declaration.Constant {
    return Swift.Declaration.Constant(name, type, value, attributes, visibility)
}

//endregion

//region Statements

fun Swift.Expression.assign(expression: Swift.Expression) = Swift.Statement.Assign(this, expression)

fun `return`(expression: Swift.Expression) = Swift.Statement.Return(expression)

val `break`
    get() = Swift.Statement.Break

val `continue`
    get() = Swift.Statement.Continue

//endregion

//region Expressions

val nil: Swift.Expression.Nil
    get() = Swift.Expression.Nil

fun Swift.Builder.variable(name: String) = Swift.Expression.Variable(name)

fun Swift.Builder.access(name: String) = Swift.Expression.Access(null, name)

fun Swift.Expression.access(name: String) = Swift.Expression.Access(this, name)

fun Swift.Expression.call(vararg arguments: Swift.Argument) = Swift.Expression.Call(this, arguments.toList())

fun Swift.Expression.call(arguments: List<Swift.Argument>) = Swift.Expression.Call(this, arguments.toList())

fun Swift.Expression.subscript(vararg arguments: Swift.Argument) = Swift.Expression.Subscript(this, arguments.toList())

//endregion

//region Types

val Swift.Builder.any: Swift.Type.Nominal
    get() = "Any".type

val Swift.Builder.anyObject: Swift.Type.Nominal
    get() = "AnyObject".type

val Swift.Builder.self: Swift.Type.Nominal
    get() = "Self".type

val Swift.Type.metatype: Swift.Type.Metatype
    get() = Swift.Type.Metatype(this)

val Swift.Type.existentialMetatype: Swift.Type.Metatype
    get() = Swift.Type.Metatype(this, isExistential = true)

val Swift.Type.opaque: Swift.Type.Opaque
    get() = Swift.Type.Opaque(this)

val Swift.Type.optional: Swift.Type.Optional
    get() = Swift.Type.Optional(this)

fun Swift.Builder.function(vararg argumentTypes: Swift.Type, returnType: Swift.Type) = Swift.Type.Function(argumentTypes.toList(), returnType)

fun Swift.Builder.tuple(vararg types: Swift.Type) = Swift.Type.Tuple(types.toList())

fun Swift.Builder.array(type: Swift.Type) = Swift.Type.Array(type)

fun Swift.Builder.dictionary(key: Swift.Type, value: Swift.Type) = Swift.Type.Dictionary(key, value)

fun Swift.Builder.dictionary(types: Pair<Swift.Type, Swift.Type>) = Swift.Type.Dictionary(types.first, types.second)

fun Swift.Builder.existential(vararg types: String) = Swift.Type.Existential(types.toList())

//endregion