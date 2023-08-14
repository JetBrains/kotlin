/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import viper.silver.ast.Method

/**
 * Contains the metadata for converting a single (specific) Kotlin method;
 * create a new instance for each method if you want to convert multiple.
 * Note that by default we do not convert the whole method: we expect to
 * only need the signature in most methods, as we verify methods one at a
 * time.
 * NOTE: since functions without contracts have not a contract description of
 * type FirResolvedContractDescription, we handle those cases by making
 * contractDescription null
 */
class MethodConverter(
    private val programCtx: ProgramConversionContext,
    override val signature: ConvertedMethodSignature,
    body: FirBlock?,
    contractDescription: FirResolvedContractDescription?
) :
    MethodConversionContext, ProgramConversionContext by programCtx {
    override val returnVar: ConvertedVar = signature.returnVar

    private var nextAnonVarNumber = 0

    override fun newAnonVar(type: ConvertedType): ConvertedVar =
        ConvertedVar(AnonymousName(++nextAnonVarNumber), type)

    // We need to make sure everything else is initialised by the time we get here.
    private val convertedBody = body?.let { convertBody(it) }

    private val convertedEffects = convertEffects(contractDescription)

    // Converting the body here would be too late; we want this to be a pure method, while
    // converting the body may involve the program context.
    override val toMethod: Method = signature.toMethod(listOf(), convertedEffects, convertedBody)

    private fun convertBody(body: FirBlock): Stmt.Seqn {
        val ctx = StmtConverter(this)
        ctx.convertAndAppend(body)
        return ctx.block
    }

    private fun convertEffects(contractDescription: FirResolvedContractDescription?): List<Exp> = contractDescription?.effects?.map {
        it.effect.accept(ContractDescriptionConversionVisitor(), this)
    }
        ?: listOf()

}