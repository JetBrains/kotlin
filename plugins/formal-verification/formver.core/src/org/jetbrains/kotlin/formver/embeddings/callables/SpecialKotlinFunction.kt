/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

interface SpecialKotlinFunction {
    // We use strings here instead of FqNames as a lightweight approximation; perhaps
    // we'll need to rethink this down the line.
    val callableId: CallableId
}

object KotlinContractFunction : SpecialKotlinFunction {
    override val callableId = CallableId(FqName.fromSegments(listOf("kotlin", "contracts")), null, Name.identifier("contract"))
}

interface SpecialKotlinFunctionImplementation : SpecialKotlinFunction {
    fun convertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding
}

object KotlinIntPlusFunctionImplementation : SpecialKotlinFunctionImplementation {
    override val callableId = CallableId(FqName("kotlin"), FqName("Int"), Name.identifier("plus"))

    override fun convertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        Add(args[0], args[1])
}

object KotlinIntMinusFunctionImplementation : SpecialKotlinFunctionImplementation {
    override val callableId = CallableId(FqName("kotlin"), FqName("Int"), Name.identifier("minus"))

    override fun convertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        Sub(args[0], args[1])
}

object KotlinIntTimesFunctionImplementation : SpecialKotlinFunctionImplementation {
    override val callableId = CallableId(FqName("kotlin"), FqName("Int"), Name.identifier("times"))

    override fun convertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        Mul(args[0], args[1])
}

object KotlinIntDivFunctionImplementation : SpecialKotlinFunctionImplementation {
    override val callableId = CallableId(FqName("kotlin"), FqName("Int"), Name.identifier("div"))

    override fun convertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        ctx.addStatement(Stmt.Inhale(NeCmp(args[1], IntLit(0)).toViper()))
        return Div(args[0], args[1])
    }
}

object KotlinBooleanNotFunctionImplementation : SpecialKotlinFunctionImplementation {
    override val callableId = CallableId(FqName("kotlin"), FqName("Boolean"), Name.identifier("not"))

    override fun convertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        Not(args[0])
}

object SpecialKotlinFunctions {
    val byCallableId = listOf(
        KotlinContractFunction,
        KotlinIntPlusFunctionImplementation,
        KotlinIntMinusFunctionImplementation,
        KotlinIntTimesFunctionImplementation,
        KotlinIntDivFunctionImplementation,
        KotlinBooleanNotFunctionImplementation,
    ).associateBy { it.callableId }
}