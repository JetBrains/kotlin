/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.names.*
import org.jetbrains.kotlin.formver.viper.ast.Method
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

    private val contractBuilderTypeName = buildName {
        packageScope(packageName)
        ClassKotlinName(listOf("ContractBuilder"))
    }
    private val contractBuilderType = ClassTypeEmbedding(contractBuilderTypeName)
    override val receiverType: TypeEmbedding? = null
    override val paramTypes: List<TypeEmbedding> =
        listOf(FunctionTypeEmbedding(CallableSignatureData(contractBuilderType, listOf(), UnitTypeEmbedding)))
    override val returnType: TypeEmbedding = UnitTypeEmbedding

    override fun insertCallImpl(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding = UnitLit
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
    override fun insertCallImpl(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding =
        Add(args[0], args[1])
}

object KotlinIntMinusFunctionImplementation : KotlinIntSpecialFunction() {
    override val name: String = "minus"
    override fun insertCallImpl(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding =
        Sub(args[0], args[1])
}

object KotlinIntTimesFunctionImplementation : KotlinIntSpecialFunction() {
    override val name: String = "times"
    override fun insertCallImpl(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding =
        Mul(args[0], args[1])
}

object KotlinIntDivFunctionImplementation : KotlinIntSpecialFunction() {
    override val name: String = "div"
    override fun insertCallImpl(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
        // TODO: implement this properly, we don't want to evaluate args[1] twice.
    ): ExpEmbedding = Block(InhaleDirect(NeCmp(args[1], IntLit(0))), Div(args[0], args[1]))
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
    override fun insertCallImpl(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding =
        Not(args[0])
}

/**
 * Represents the `verify` function defined in `org.jetbrains.kotlin.formver.plugin`.
 */
object SpecialVerifyFunction : SpecialKotlinFunction {
    override val packageName: List<String> = listOf("org", "jetbrains", "kotlin", "formver", "plugin")
    override val name: String = "verify"

    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding {
        return Assert(args[0])
    }

    override val receiverType: TypeEmbedding? = null
    override val paramTypes: List<TypeEmbedding> = listOf(BooleanTypeEmbedding)
    override val returnType: TypeEmbedding = UnitTypeEmbedding
}

object SpecialKotlinFunctions {
    val byName = listOf(
        SpecialVerifyFunction,
        KotlinContractFunction,
        KotlinIntPlusFunctionImplementation,
        KotlinIntMinusFunctionImplementation,
        KotlinIntTimesFunctionImplementation,
        KotlinIntDivFunctionImplementation,
        KotlinBooleanNotFunctionImplementation,
    ).associateBy { it.embedName() }
}