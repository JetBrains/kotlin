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
import org.jetbrains.kotlin.formver.embeddings.expression.debug.*
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
            exp.toViperUnusedResult(ctx)
        }
        exps.last().toViperMaybeStoringIn(result, ctx)
    }

    override val debugTreeView: TreeView
        get() = when (exps.size) {
            0 -> PlaintextLeaf("EmptyBlock")
            1 -> exps[0].debugTreeView
            else -> NamedBranchingNode("Block", exps.map { it.debugTreeView })
        }
}

data class If(val condition: ExpEmbedding, val thenBranch: ExpEmbedding, val elseBranch: ExpEmbedding, override val type: TypeEmbedding) :
    OptionalResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperMaybeStoringIn(result: Exp.LocalVar?, ctx: LinearizationContext) {
        val condViper = condition.toViper(ctx)
        val thenViper = ctx.asBlock { thenBranch.toViperMaybeStoringIn(result, this) }
        val elseViper = ctx.asBlock { elseBranch.toViperMaybeStoringIn(result, this) }
        ctx.addStatement(Stmt.If(condViper, thenViper, elseViper, ctx.source.asPosition))
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(condition, thenBranch, elseBranch)
}

data class While(
    val condition: ExpEmbedding,
    val body: ExpEmbedding,
    val breakLabel: Label,
    val continueLabel: Label,
    val invariants: List<ExpEmbedding>,
) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = UnitTypeEmbedding

    override fun toViperSideEffects(ctx: LinearizationContext) {
        val condVar = ctx.freshAnonVar(BooleanTypeEmbedding)
        ctx.addLabel(continueLabel)
        condition.toViperStoringIn(condVar, ctx)
        val bodyBlock = ctx.asBlock {
            body.toViperUnusedResult(this)
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
        ctx.addLabel(breakLabel)
    }

    // TODO: add invariants
    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(condition, body)

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(
            breakLabel.debugTreeView.withDesignation("break"),
            continueLabel.debugTreeView.withDesignation("continue"),
        )
}

data class Goto(val target: Label) : NoResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = NothingTypeEmbedding
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        ctx.addStatement(target.toGoto(ctx.source.asPosition))
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf()

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(target.debugTreeView)
}

// Using this name to avoid clashes with all our other `Label` types.
data class LabelExp(val label: Label) : UnitResultExpEmbedding {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addLabel(label)
    }

    override val debugTreeView: TreeView
        get() = NamedBranchingNode("Label", label.debugTreeView)
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

    override val debugTreeView: TreeView
        get() = NamedBranchingNode("GotoChainNode", listOfNotNull())
}

data class NonDeterministically(val exp: ExpEmbedding) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        val choice = ctx.freshAnonVar(BooleanTypeEmbedding)
        val expViper = ctx.asBlock { exp.toViper(this) }
        ctx.addStatement(Stmt.If(choice, expViper, Stmt.Seqn(), ctx.source.asPosition))
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(exp)
}

// Note: this is always a *real* Viper method call.
data class MethodCall(val method: NamedFunctionSignature, val args: List<ExpEmbedding>) : StoredResultExpEmbedding {
    override val type: TypeEmbedding = method.returnType

    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        TODO("Need to modify conversion code to get this working.")
    }

    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "MethodCall",
            buildList {
                add(method.nameAsDebugTreeView.withDesignation("callee"))
                addAll(args.map { it.debugTreeView })
            })
}

/**
 * We need to generate a fresh variable here since we want to havoc the result.
 *
 * TODO: do this with an explicit havoc in `toViperMaybeStoringIn`.
 */
data class InvokeFunctionObject(val receiver: ExpEmbedding, val args: List<ExpEmbedding>, override val type: TypeEmbedding) :
    OnlyToViperExpEmbedding {
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
            )
        )
        return variable
    }

    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "InvokeFunctionObject",
            buildList {
                add(receiver.debugTreeView.withDesignation("receiver"))
                addAll(args.map { it.debugTreeView })
            })
}

data class FunctionExp(val signature: FullNamedFunctionSignature?, val body: ExpEmbedding, val returnLabel: Label) :
    OptionalResultExpEmbedding {
    override val type: TypeEmbedding = body.type

    override fun toViperMaybeStoringIn(result: Exp.LocalVar?, ctx: LinearizationContext) {
        signature?.formalArgs?.forEach { arg ->
            // Ideally we would want to assume these rather than inhale them to prevent inconsistencies with permissions.
            // Unfortunately Silicon for some reason does not allow Assumes. However, it doesn't matter as long as the
            // provenInvariants don't contain permissions.
            arg.provenInvariants().forEach { invariant ->
                ctx.addStatement(Stmt.Inhale(invariant.pureToViper()))
            }
        }
        body.toViperMaybeStoringIn(result, ctx)
        ctx.addLabel(returnLabel)
    }

    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "Function",
            listOfNotNull(
                signature?.nameAsDebugTreeView?.withDesignation("name"),
                body.debugTreeView,
                returnLabel.debugTreeView.withDesignation("return")
            )
        )
}