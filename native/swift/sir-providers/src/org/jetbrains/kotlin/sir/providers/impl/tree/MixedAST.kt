/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.tree

import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirTypeNamer

/**
 * This class represents simple AST, that is used in TypeBridging.kt
 * for Swift & Kotlin generated code representation
 */
internal sealed class MixedAST {
    class Ref(val expression: String) : MixedAST() {
        override fun toString(): String = expression
    }

    class RefWithTypeArguments(val ref: Ref, val typeArguments: Arguments) : MixedAST() {
        override fun toString(): String = "$ref<$typeArguments>"
    }

    class BinaryOp(val left: MixedAST, val operator: Operator, val right: MixedAST) : MixedAST() {
        override fun toString(): String = "$left$operator$right"

        fun replace(operator: Operator = this.operator, transform: (MixedAST) -> MixedAST) =
            BinaryOp(transform(left), operator, transform(right))
    }

    class Conditional(val condition: MixedAST, val ifTrue: MixedAST, val ifFalse: MixedAST) : MixedAST() {
        override fun toString(): String = "if ($condition) $ifTrue else $ifFalse"
    }

    class Variable(val identifier: MixedAST, val isSwift: Boolean) : MixedAST() {
        override fun toString(): String = "${if (isSwift) "let" else "val"} $identifier"
    }

    class Case(val branch: MixedAST, val result: MixedAST) : MixedAST() {
        override fun toString(): String = "case $branch: $result;"
    }

    class Switch(val subject: MixedAST, val cases: List<Case>) : MixedAST() {
        override fun toString(): String = buildString {
            append("switch $subject { ")
            append(cases.joinToString(separator = " "))
            append(" }")
        }
    }

    class Block(val lambdaParameters: LambdaParameters?, val statements: List<MixedAST>, val separateLines: Boolean) : MixedAST() {
        override fun toString() = buildString {
            append("{ ")
            if (lambdaParameters != null) {
                append("$lambdaParameters ")
            }
            if (separateLines) {
                appendLine()
            }
            if (separateLines) {
                for ((index, statement) in statements.withIndex()) {
                    append("|    ")
                    append(statement)
                    val nextIsBlock = statements.getOrNull(index + 1) is Block
                    appendLine(if (nextIsBlock) ";" else "")
                }
            } else {
                append(statements.joinToString(separator = "; "))
            }
            if (!separateLines) {
                append(" ")
            } else {
                append("|")
            }
            append("}")
        }
    }

    class Invoke(val callee: MixedAST, val arguments: Arguments) : MixedAST() {
        override fun toString(): String = "$callee($arguments)"
    }

    class InvokeLambda(val callee: MixedAST, val argument: Block) : MixedAST() {
        override fun toString(): String = "$callee $argument"
    }

    class Return(val argument: MixedAST) : MixedAST() {
        override fun toString(): String = "return $argument"
    }

    class ExclExcl(val argument: MixedAST) : MixedAST() {
        override fun toString(): String = "$argument!!"
    }

    class Addr(val argument: MixedAST) : MixedAST() {
        override fun toString(): String = "&$argument"
    }

    class LambdaParameters(val parameters: List<MixedAST>, val isSwift: Boolean) : MixedAST() {
        override fun toString(): String = parameters.joinToString() + if (isSwift) " in" else " ->"
    }

    class Arguments(val arguments: List<MixedAST>) : MixedAST() {
        override fun toString(): String = arguments.joinToString()
    }

    object Unit : MixedAST() {
        override fun toString(): String = "Unit"
    }

    object Null : MixedAST() {
        override fun toString(): String = "null"
    }

    object Nil : MixedAST() {
        override fun toString(): String = "nil"
    }

    object NativeNull : MixedAST() {
        override fun toString(): String = "kotlin.native.internal.NativePtr.NULL"
    }

    object NsNull : MixedAST() {
        override fun toString(): String = "NSNull"
    }

    object None : MixedAST() {
        override fun toString(): String = ".none"
    }

    object NsObjectNullable : MixedAST() {
        override fun toString(): String = "NSObject?"
    }

    object KotlinAny : MixedAST() {
        override fun toString(): String = "kotlin.Any"
    }

    enum class Operator(private val representation: String) {
        COMMA(", "),
        COLON(": "),
        ACCESS("."),
        SAFE_ACCESS("?."),
        AS(" as "),
        AS_EXCL(" as! "),
        ASSIGN(" = "),
        EQUALS(" == "),
        IN(" in "),
        SWIFT_ELVIS(" ?? "),
        RANGE_KOTLIN(" .. "),
        RANGE_SWIFT(" ... "),
        RANGE_UNTIL(" ..< "),
        ;

        override fun toString(): String = representation
    }
}

internal fun String.ast(): MixedAST.Ref = MixedAST.Ref(this)

internal fun MixedAST.parameterX(index: Int): MixedAST {
    assert(this is MixedAST.Ref)
    return MixedAST.Ref("${this}_$index")
}

internal fun MixedAST.access(name: String) = MixedAST.BinaryOp(this, MixedAST.Operator.ACCESS, name.ast())

internal fun MixedAST.safeAccess(name: String) = MixedAST.BinaryOp(this, MixedAST.Operator.SAFE_ACCESS, name.ast())

internal fun MixedAST.externalRCRef() = access("__externalRCRef").invoke()

internal fun MixedAST.dereferenceExternalRCRef() = "kotlin.native.internal.ref.dereferenceExternalRCRef".ast().invoke(this)

internal fun MixedAST.createRetainedExternalRCRef() = "kotlin.native.internal.ref.createRetainedExternalRCRef".ast().invoke(this)

internal fun MixedAST.objcPtr() = access("objcPtr").invoke()

internal fun MixedAST.interpretObjCPointer(type: SirType, namer: SirTypeNamer) =
    interpretObjCPointer(type.kotlinTypeName(namer).toString())

internal fun MixedAST.interpretObjCPointer(typeName: String) =
    MixedAST.RefWithTypeArguments(
        "interpretObjCPointer".ast(),
        MixedAST.Arguments(listOf(typeName.ast()))
    ).invoke(this)

internal fun MixedAST.convertBlockPtrToKotlinFunction(typeName: String) =
    MixedAST.RefWithTypeArguments(
        "convertBlockPtrToKotlinFunction".ast(),
        MixedAST.Arguments(listOf(typeName.ast()))
    ).invoke(this)

internal fun MixedAST.invoke(vararg arguments: MixedAST): MixedAST.Invoke =
    MixedAST.Invoke(this, MixedAST.Arguments(arguments.toList()))

internal fun brackets(vararg arguments: MixedAST): MixedAST =
    "".ast().invoke(*arguments)

internal fun MixedAST.invokeLambda(
    parameters: MixedAST.LambdaParameters? = null,
    separateLines: Boolean = false,
    f: BlockBuilder.() -> Unit,
) = MixedAST.InvokeLambda(this, lambda(parameters, separateLines = separateLines, f))

internal fun String.variable(isSwift: Boolean): MixedAST.Variable = MixedAST.Variable(ast(), isSwift)

internal fun MixedAST.named(name: String): MixedAST = name.ast().op(MixedAST.Operator.COLON, this)

internal fun asBestFittingWrapper() = ".asBestFittingWrapper".ast().named("options")

internal fun ret(ast: MixedAST) = MixedAST.Return(ast)

internal fun ret(s: String) = ret(s.ast())

internal fun MixedAST.exclExcl() = MixedAST.ExclExcl(this)

internal fun MixedAST.asKotlin(namer: SirTypeNamer, type: SirType) =
    asKotlin(type.kotlinTypeName(namer))

internal fun MixedAST.asKotlin(typeAST: MixedAST) =
    op(MixedAST.Operator.AS, typeAST)

internal fun MixedAST.asSwift(namer: SirTypeNamer, type: SirType) =
    asSwift(type.swiftTypeName(namer))

internal fun MixedAST.asSwift(typeAST: MixedAST) =
    op(MixedAST.Operator.AS_EXCL, typeAST)

internal fun MixedAST.addr() = MixedAST.Addr(this)

internal fun MixedAST.eq(right: MixedAST) = op(MixedAST.Operator.EQUALS, right)

internal fun MixedAST.op(op: MixedAST.Operator, right: MixedAST) = MixedAST.BinaryOp(this, op, right)

internal fun MixedAST.cond(ifTrue: MixedAST, ifFalse: MixedAST) = MixedAST.Conditional(this, ifTrue, ifFalse)

internal fun SirType.kotlinTypeName(namer: SirTypeNamer): MixedAST =
    namer.kotlinFqName(this, SirTypeNamer.KotlinNameType.PARAMETRIZED).ast()

internal fun SirType.swiftTypeName(namer: SirTypeNamer): MixedAST =
    namer.swiftFqName(this).ast()

internal fun SirType.createProtocolWrapper(namer: SirTypeNamer, expression: MixedAST): MixedAST =
    swiftTypeName(namer).access("__createProtocolWrapper").invoke(expression.named("externalRCRef"))

internal fun SirType.createClassWrapper(namer: SirTypeNamer, expression: MixedAST): MixedAST =
    swiftTypeName(namer).access("__createClassWrapper").invoke(expression.named("externalRCRef"))

internal fun SirType.create(namer: SirTypeNamer, expression: MixedAST): MixedAST =
    swiftTypeName(namer).invoke(expression.named("__externalRCRefUnsafe"), asBestFittingWrapper())

internal fun block(
    separateLines: Boolean = false,
    f: BlockBuilder.() -> Unit,
): MixedAST.Block {
    return BlockBuilder(null, separateLines).apply { f() }.block
}

internal fun lambda(
    lambdaParameters: MixedAST.LambdaParameters?,
    separateLines: Boolean = false,
    f: BlockBuilder.() -> Unit,
): MixedAST.Block {
    return BlockBuilder(lambdaParameters, separateLines).apply { f() }.block
}

internal fun switch(ast: MixedAST, f: SwitchBuilder.() -> Unit): MixedAST.Switch {
    return SwitchBuilder(ast).apply { f() }.switch
}

internal class BlockBuilder(val lambdaParameters: MixedAST.LambdaParameters?, val separateLines: Boolean) {
    private val statements: MutableList<MixedAST> = mutableListOf()

    operator fun MixedAST.unaryPlus() {
        add(this)
    }

    fun add(statement: MixedAST) {
        statements += statement
    }

    fun add(s: String) {
        add(s.ast())
    }

    val block: MixedAST.Block
        get() {
            return MixedAST.Block(lambdaParameters, statements, separateLines)
        }
}

internal class SwitchBuilder(val subject: MixedAST) {
    private val cases: MutableList<MixedAST.Case> = mutableListOf()

    fun case(branch: MixedAST, result: MixedAST) {
        cases.add(MixedAST.Case(branch, result))
    }

    val switch: MixedAST.Switch
        get() = MixedAST.Switch(subject, cases)
}
