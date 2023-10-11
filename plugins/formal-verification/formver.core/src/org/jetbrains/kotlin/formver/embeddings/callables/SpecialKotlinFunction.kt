/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Kotlin function that should be handled specially by our conversion.
 *
 * This includes `contract` and operations on primitive types, where providing a full embedding into Viper
 * offers more possibilities for reasoning about the code.
 */
interface SpecialKotlinFunction : FunctionEmbedding {
    val packageName: List<String>
    val className: String?
        get() = null
    val name: String
    override val viperMethod: Method?
        get() = null
}

val SpecialKotlinFunction.callableId: CallableId
    get() = CallableId(FqName.fromSegments(packageName), className?.let { FqName(it) }, Name.identifier(name))

fun SpecialKotlinFunction.embedName(): ScopedKotlinName = callableId.embedFunctionName(FunctionTypeEmbedding(asData))

object KotlinContractFunction : SpecialKotlinFunction {
    override val packageName: List<String> = listOf("kotlin", "contracts")
    override val name: String = "contract"

    private val contractBuilderType =
        ClassTypeEmbedding(
            ScopedKotlinName(GlobalScope(packageName), ClassKotlinName(Name.identifier("ContractBuilder"))),
            listOf(AnyTypeEmbedding)
        )
    override val receiverType: TypeEmbedding? = null
    override val paramTypes: List<TypeEmbedding> =
        listOf(FunctionTypeEmbedding(CallableSignatureData(contractBuilderType, listOf(), UnitTypeEmbedding)))
    override val returnType: TypeEmbedding = UnitTypeEmbedding

    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding = UnitLit
}

abstract class KotlinIntSpecialFunction : SpecialKotlinFunction {
    override val packageName: List<String> = listOf("kotlin")
    override val className: String? = "Int"

    override val receiverType: TypeEmbedding = IntTypeEmbedding
    override val paramTypes: List<TypeEmbedding> = listOf(IntTypeEmbedding)
    override val returnType: TypeEmbedding = IntTypeEmbedding
}

object KotlinIntPlusFunctionImplementation : KotlinIntSpecialFunction() {
    override val name: String = "plus"
    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        Add(args[0], args[1])
}

object KotlinIntMinusFunctionImplementation : KotlinIntSpecialFunction() {
    override val name: String = "minus"
    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        Sub(args[0], args[1])
}

object KotlinIntTimesFunctionImplementation : KotlinIntSpecialFunction() {
    override val name: String = "times"
    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        Mul(args[0], args[1])
}

object KotlinIntDivFunctionImplementation : KotlinIntSpecialFunction() {
    override val name: String = "div"
    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        ctx.addStatement(Stmt.Inhale(NeCmp(args[1], IntLit(0)).toViper()))
        return Div(args[0], args[1])
    }
}

abstract class KotlinBooleanSpecialFunction : SpecialKotlinFunction {
    override val packageName: List<String> = listOf("kotlin")
    override val className: String? = "Boolean"

    override val receiverType: TypeEmbedding = BooleanTypeEmbedding
    override val paramTypes: List<TypeEmbedding> = emptyList()
    override val returnType: TypeEmbedding = BooleanTypeEmbedding
}

object KotlinBooleanNotFunctionImplementation : KotlinBooleanSpecialFunction() {
    override val name: String = "not"
    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        Not(args[0])
}

object KotlinRunSpecialFunction : SpecialKotlinFunction {
    override val packageName: List<String> = listOf("kotlin")
    override val name: String = "run"

    override val receiverType: TypeEmbedding? = null
    override val paramTypes: List<TypeEmbedding> =
        listOf(FunctionTypeEmbedding(CallableSignatureData(null, emptyList(), NullableTypeEmbedding(AnyTypeEmbedding))))
    override val returnType: TypeEmbedding = NullableTypeEmbedding(AnyTypeEmbedding)

    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        val lambda = when (val arg = args[0].ignoringCasts()) {
            is LambdaExp -> arg
            else -> throw IllegalStateException("kotlin.run must be called with a lambda argument at the moment")
        }

        return lambda.insertCallImpl(listOf(), ctx)
    }
}

object SpecialKotlinFunctions {
    val byName = listOf(
        KotlinContractFunction,
        KotlinIntPlusFunctionImplementation,
        KotlinIntMinusFunctionImplementation,
        KotlinIntTimesFunctionImplementation,
        KotlinIntDivFunctionImplementation,
        KotlinBooleanNotFunctionImplementation,
        KotlinRunSpecialFunction,
    ).associateBy { it.embedName() }
}