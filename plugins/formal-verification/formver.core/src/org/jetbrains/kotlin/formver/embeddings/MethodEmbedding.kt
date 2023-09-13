/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLambdaArgumentExpression
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.conversion.*
import org.jetbrains.kotlin.formver.domains.convertType
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*

interface MethodEmbedding : MethodSignatureEmbedding {
    val preconditions: List<Exp>
    val postconditions: List<Exp>

    val shouldIncludeInProgram: Boolean
    val viperMethod: Method

    fun convertBody(ctx: ProgramConverter)
    fun insertCall(argsFir: List<FirExpression>, ctx: StmtConversionContext<ResultTrackingContext>): VariableEmbedding

    fun getFunctionCallSubstitutionItems(
        args: List<FirExpression>,
        data: StmtConversionContext<ResultTrackingContext>
    ): List<SubstitutionItem>
}

class UserMethodEmbedding(
    val signature: MethodSignatureEmbedding,
    override val preconditions: List<Exp>,
    override val postconditions: List<Exp>,
    val symbol: FirFunctionSymbol<*>,
) : MethodEmbedding, MethodSignatureEmbedding by signature {
    var body: Stmt.Seqn? = null
    override val shouldIncludeInProgram
        get() = !symbol.isInline || body != null

    override val viperMethod
        get() = UserMethod(
            name,
            formalArgs.map { it.toLocalVarDecl() },
            returnVar.toLocalVarDecl(),
            preconditions,
            postconditions,
            body
        )

    @OptIn(SymbolInternals::class)
    override fun convertBody(ctx: ProgramConverter) {
        val methodCtx = object : MethodConversionContext, ProgramConversionContext by ctx {
            override val method: MethodEmbedding = this@UserMethodEmbedding

            // It seems like Viper will propagate the weakest precondition through the label correctly even in the absence of
            // explicit invariants; we only need to add those if we want to make a stronger claim.
            override val returnLabel: Label = Label(ReturnLabelName, listOf())
            override val returnVar: VariableEmbedding = VariableEmbedding(ReturnVariableName, method.returnType)

            override fun resolveName(name: MangledName): MangledName = name
        }

        body = symbol.fir.body?.let {
            val stmtCtx = StmtConverter(methodCtx, SeqnBuilder(), NoopResultTrackerFactory)
            signature.formalArgs.forEach { arg ->
                // Ideally we would want to assume these rather than inhale them to prevent inconsistencies with permissions.
                // Unfortunately Silicon for some reason does not allow Assumes. However, it doesn't matter as long as the
                // provenInvariants don't contain permissions.
                arg.provenInvariants().forEach { invariant ->
                    stmtCtx.addStatement(Stmt.Inhale(invariant))
                }
            }
            stmtCtx.addDeclaration(methodCtx.returnLabel.toDecl())
            stmtCtx.convert(it)
            stmtCtx.addStatement(methodCtx.returnLabel.toStmt())
            stmtCtx.block
        }
    }

    @OptIn(SymbolInternals::class)
    override fun insertCall(argsFir: List<FirExpression>, ctx: StmtConversionContext<ResultTrackingContext>): VariableEmbedding =
        ctx.withResult(returnType) {
            if (!symbol.isInline) {
                val args = argsFir
                    .zip(formalArgs)
                    .map { (arg, formalArg) -> convert(arg).withType(formalArg.type) }

                addStatement(toMethodCall(args.toViper(), this.resultCtx.resultVar))
            } else {
                val inlineBody = symbol.fir.body ?: throw Exception("Function symbol $symbol has a null body")
                val inlineBodyCtx = newBlock()
                val inlineArgs: List<MangledName> = symbol.valueParameterSymbols.map { it.embedName() }
                val callArgs = getFunctionCallSubstitutionItems(argsFir, inlineBodyCtx)
                val substitutionParams = inlineArgs.zip(callArgs).toMap()

                val inlineCtx = inlineBodyCtx.withInlineContext(
                    this@UserMethodEmbedding,
                    inlineBodyCtx.resultCtx.resultVar,
                    substitutionParams
                )
                inlineCtx.convert(inlineBody)
                // TODO: add these labels automatically.
                inlineCtx.addDeclaration(inlineCtx.returnLabel.toDecl())
                inlineCtx.addStatement(inlineCtx.returnLabel.toStmt())
                // Note: Putting the block inside the then branch of an if-true statement is a little a hack to make Viper respect the scoping
                addStatement(Stmt.If(Exp.BoolLit(true), inlineCtx.block, Stmt.Seqn(listOf(), listOf())))
            }
        }

    override fun getFunctionCallSubstitutionItems(
        args: List<FirExpression>,
        data: StmtConversionContext<ResultTrackingContext>
    ): List<SubstitutionItem> = args.map { exp ->
        when (exp) {
            is FirLambdaArgumentExpression -> {
                val anonExpr = exp.expression
                if (anonExpr is FirAnonymousFunctionExpression) {
                    val lambdaBody = anonExpr.anonymousFunction.body!!
                    val lambdaArs = anonExpr.anonymousFunction.valueParameters.map { LocalName(it.name) }
                    SubstitutionLambda(lambdaBody, lambdaArs)
                } else {
                    TODO("are there any other cases?")
                }
            }
            else -> SubstitutionName(data.convertAndStore(exp).name)
        }
    }
}