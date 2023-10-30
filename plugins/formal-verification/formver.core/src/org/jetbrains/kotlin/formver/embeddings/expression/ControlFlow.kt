/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.embeddings.BooleanTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.NothingTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.UnitTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.embeddings.callables.InvokeFunctionObjectMethod
import org.jetbrains.kotlin.formver.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.linearization.addLabel
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.formver.viper.ast.Stmt

// TODO: make a nice BlockBuilder interface.
data class Block(val exps: List<ExpEmbedding>) : OptionalResultExpEmbedding {
    constructor (vararg exps: ExpEmbedding) : this(exps.toList())

    override val type: TypeEmbedding = exps.lastOrNull()?.type ?: UnitTypeEmbedding

    override fun toViperMaybeStoringIn(result: Exp.LocalVar?, ctx: LinearizationContext) {
        if (exps.isEmpty()) return

        for (exp in exps.take(exps.size - 1)) {
            exp.toViper(ctx)
        }
        exps.last().toViperMaybeStoringIn(result, ctx)
    }
}

data class If(val condition: ExpEmbedding, val thenBranch: ExpEmbedding, val elseBranch: ExpEmbedding, override val type: TypeEmbedding) :
    OptionalResultExpEmbedding {
    override fun toViperMaybeStoringIn(result: Exp.LocalVar?, ctx: LinearizationContext) {
        val condViper = condition.toViper(ctx)
        val thenViper = ctx.asBlock { thenBranch.toViperMaybeStoringIn(result, this) }
        val elseViper = ctx.asBlock { elseBranch.toViperMaybeStoringIn(result, this) }
        ctx.addStatement(Stmt.If(condViper, thenViper, elseViper, ctx.source.asPosition))
    }
}

data class While(
    val condition: ExpEmbedding,
    val body: ExpEmbedding,
    val breakLabel: Label,
    val continueLabel: Label,
    val invariants: List<ExpEmbedding>,
) : UnitResultExpEmbedding {
    override val type: TypeEmbedding = UnitTypeEmbedding

    override fun toViperSideEffects(ctx: LinearizationContext) {
        val condVar = ctx.freshAnonVar(BooleanTypeEmbedding)
        ctx.addLabel(continueLabel)
        condition.toViperStoringIn(condVar, ctx)
        val bodyBlock = ctx.asBlock {
            body.toViper(this)
            condition.toViperStoringIn(condVar, this)
        }
        ctx.addStatement(
            Stmt.While(
                condVar,
                invariants.pureToViper(ctx.source),
                bodyBlock,
                ctx.source.asPosition
            )
        )
    }
}

data class Return(val returnVariable: VariableEmbedding, val returnTarget: Label, val returnValue: ExpEmbedding) : NoResultExpEmbedding {
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        returnValue.toViperStoringIn(returnVariable.toLocalVarUse(), ctx)
        ctx.addStatement(returnTarget.toGoto(ctx.source.asPosition))
    }
}

data class Goto(val target: Label) : NoResultExpEmbedding {
    override val type: TypeEmbedding = NothingTypeEmbedding
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        ctx.addStatement(target.toGoto(ctx.source.asPosition))
    }
}

// Using this name to avoid clashes with all our other `Label` types.
data class LabelExp(val label: Label) : UnitResultExpEmbedding {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addLabel(label)
    }
}

/**
 * An expression that optionally has a label and that uses a goto to exit.
 *
 * The result of the intermediate expression is stored.
 */
data class GotoChainNode(val label: Label?, val exp: ExpEmbedding, val next: Label) : OptionalResultExpEmbedding {
    override val type: TypeEmbedding = exp.type

    override fun toViperMaybeStoringIn(result: Exp.LocalVar?, ctx: LinearizationContext) {
        label?.let { ctx.addLabel(it) }
        exp.toViperMaybeStoringIn(result, ctx)
        ctx.addStatement(next.toGoto())
    }
}

data class NonDeterministically(val exp: ExpEmbedding): UnitResultExpEmbedding {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        val choice = ctx.freshAnonVar(BooleanTypeEmbedding)
        val expViper = ctx.asBlock { exp.toViper(this) }
        ctx.addStatement(Stmt.If(choice, expViper, Stmt.Seqn(), ctx.source.asPosition))
    }
}

// Note: this is always a *real* Viper method call.
data class MethodCall(val method: NamedFunctionSignature, val args: List<ExpEmbedding>) : StoredResultExpEmbedding {
    override val type: TypeEmbedding = method.returnType

    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        TODO("Need to modify conversion code to get this working.")
    }
}

data class InvokeFunctionObject(val receiver: ExpEmbedding, val args: List<ExpEmbedding>, override val type: TypeEmbedding) : DirectResultExpEmbedding {
    override fun toViper(ctx: LinearizationContext): Exp {
        val receiverViper = receiver.toViper(ctx)
        for (arg in args) arg.toViperUnusedResult(ctx)
        val variable = ctx.freshAnonVar(type)
        // NOTE: Since it is only relevant to update the number of times that a function object is called,
        // the function call invocation is intentionally not assigned to the return variable
        ctx.addStatement(
            InvokeFunctionObjectMethod.toMethodCall(
                listOf(receiverViper),
                listOf(),
                ctx.source.asPosition
            ))
        return variable
    }
}

data class FunctionExp(val signature: FullNamedFunctionSignature?, val body: ExpEmbedding, val returnLabel: Label) : UnitResultExpEmbedding {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        signature?.formalArgs?.forEach { arg ->
            // Ideally we would want to assume these rather than inhale them to prevent inconsistencies with permissions.
            // Unfortunately Silicon for some reason does not allow Assumes. However, it doesn't matter as long as the
            // provenInvariants don't contain permissions.
            arg.provenInvariants().forEach { invariant ->
                ctx.addStatement(Stmt.Inhale(invariant.pureToViper()))
            }
        }
        body.toViperUnusedResult(ctx)
        ctx.addLabel(returnLabel)
    }
}