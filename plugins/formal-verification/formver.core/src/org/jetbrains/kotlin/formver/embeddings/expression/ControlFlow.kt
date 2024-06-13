/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.embeddings.callables.InvokeFunctionObjectMethod
import org.jetbrains.kotlin.formver.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.embeddings.callables.toMethodCall
import org.jetbrains.kotlin.formver.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.linearization.addLabel
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.*

// TODO: make a nice BlockBuilder interface.
data class Block(val exps: List<ExpEmbedding>) : OptionalResultExpEmbedding {
    constructor (vararg exps: ExpEmbedding) : this(exps.toList())

    override val type: TypeEmbedding = exps.lastOrNull()?.type ?: UnitTypeEmbedding

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (exps.isEmpty()) return

        for (exp in exps.take(exps.size - 1)) {
            exp.toViperUnusedResult(ctx)
        }
        exps.last().toViperMaybeStoringIn(result, ctx)
    }

    override val debugTreeView: TreeView
        get() = BlockNode(exps.map { it.debugTreeView })
}

data class If(val condition: ExpEmbedding, val thenBranch: ExpEmbedding, val elseBranch: ExpEmbedding, override val type: TypeEmbedding) :
    OptionalResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        ctx.addStatement {
            val condViper = condition.toViperBuiltinType(ctx)
            val thenViper = ctx.asBlock { thenBranch.withType(type).toViperMaybeStoringIn(result, this) }
            val elseViper = ctx.asBlock { elseBranch.withType(type).toViperMaybeStoringIn(result, this) }
            Stmt.If(condViper, thenViper, elseViper, ctx.source.asPosition)
        }
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
        ctx.addLabel(continueLabel)
        ctx.addStatement {
            val condVar = ctx.freshAnonVar(BooleanTypeEmbedding)
            condition.toViperStoringIn(condVar, ctx)
            val bodyBlock = ctx.asBlock {
                body.toViperUnusedResult(this)
                condition.toViperStoringIn(condVar, this)
            }
            Stmt.While(
                condVar.toViperBuiltinType(ctx),
                invariants.pureToViper(toBuiltin = true, ctx.source),
                bodyBlock,
                ctx.source.asPosition
            )
        }
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
        ctx.addStatement { target.toGoto(ctx.source.asPosition) }
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

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        label?.let { ctx.addLabel(it) }
        ctx.addStatement {
            exp.toViperMaybeStoringIn(result, ctx)
            next.toGoto(ctx.source.asPosition)
        }
    }

    override val debugTreeView: TreeView
        get() = NamedBranchingNode("GotoChainNode", listOfNotNull())
}

data class NonDeterministically(val exp: ExpEmbedding) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addStatement {
            val choice = ctx.freshAnonVar(BooleanTypeEmbedding)
            val expViper = ctx.asBlock { exp.toViper(this) }
            Stmt.If(choice.toViperBuiltinType(ctx), expViper, Stmt.Seqn(), ctx.source.asPosition)
        }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(exp)
}

data class UnfoldingClassPredicateEmbedding(val predicated: VariableEmbedding, override val inner: ExpEmbedding) :
    UnaryDirectResultExpEmbedding {
    override val type: TypeEmbedding = inner.type
    private fun toViperImpl(ctx: LinearizationContext, action: ExpEmbedding.() -> Exp): Exp {
        val predicatedType = predicated.type
        check(predicatedType is ClassTypeEmbedding) {
            "Built-in types do not have predicates."
        }
        return Exp.Unfolding(
            Exp.PredicateAccess(
                predicatedType.predicate.name,
                listOf(predicated.pureToViper(false)),
                PermExp.WildcardPerm(),
                pos = ctx.source.asPosition,
                info = sourceRole.asInfo
            ),
            inner.action()
        )
    }

    override fun toViper(ctx: LinearizationContext): Exp = toViperImpl(ctx) {
        toViper(ctx)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp = toViperImpl(ctx) {
        toViperBuiltinType(ctx)
    }
}

// Note: this is always a *real* Viper method call.
data class MethodCall(val method: NamedFunctionSignature, val args: List<ExpEmbedding>) : StoredResultExpEmbedding {
    override val type: TypeEmbedding = method.returnType

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        ctx.addStatement {
            method.toMethodCall(
                args.map { it.toViper(ctx) },
                result.toLocalVarUse(ctx.source.asPosition),
                ctx.source.asPosition
            )
        }
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
        val variable = ctx.freshAnonVar(type)
        ctx.addStatement {
            val receiverViper = receiver.toViper(ctx)
            for (arg in args) arg.toViperUnusedResult(ctx)
            // NOTE: Since it is only relevant to update the number of times that a function object is called,
            // the function call invocation is intentionally not assigned to the return variable
            InvokeFunctionObjectMethod.toMethodCall(
                listOf(receiverViper),
                listOf(),
                ctx.source.asPosition
            )
        }
        // TODO: figure out which exactly invariants we want here
        return variable.withAccessAndProvenInvariants().toViper(ctx)
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

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        signature?.formalArgs?.forEach { arg ->
            // Ideally we would want to assume these rather than inhale them to prevent inconsistencies with permissions.
            // Unfortunately Silicon for some reason does not allow Assumes. However, it doesn't matter as long as the
            // provenInvariants don't contain permissions.
            // TODO (inhale vs require) Decide if `predicateAccessInvariant` should be required rather than inhaled in the beginning of the body.
            (arg.provenInvariants() + listOfNotNull(arg.predicateAccessInvariant())).forEach { invariant ->
                ctx.addStatement { Stmt.Inhale(invariant.toViperBuiltinType(ctx), ctx.source.asPosition) }
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

data class Elvis(val left: ExpEmbedding, val right: ExpEmbedding, override val type: TypeEmbedding) : StoredResultExpEmbedding,
    DefaultDebugTreeViewImplementation {
    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        val leftViper = left.toViper(ctx)
        val leftWrapped = ExpWrapper(leftViper, left.type)
        val conditional = If(leftWrapped.notNullCmp(), leftWrapped, right, type)
        conditional.toViperStoringIn(result, ctx)
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(left, right)
}