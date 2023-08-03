/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.seqOf
import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import org.jetbrains.kotlin.formver.scala.toScalaBigInt
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.ConstantValueKind
import viper.silver.ast.Declaration
import viper.silver.ast.LocalVarDecl
import viper.silver.ast.Method
import viper.silver.ast.Program

const val INT_BACKING_FIELD = "backing_int"

class ConvertedVar(val name: ConvertedName, val type: ConvertedType) {
    fun toLocalVarDecl(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): LocalVarDecl = localVarDecl(name.asString, type.viperType, pos, info, trafos)

    fun toLocalVar(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.LocalVar = Exp.LocalVar(name.asString, type.viperType, pos, info, trafos)

    fun preconditions(): List<Exp> = type.preconditions(toLocalVar())
    fun postconditions(): List<Exp> = type.postconditions(toLocalVar())
}

class ConvertedMethodSignature(val name: ConvertedName, val params: List<ConvertedVar>, val returns: List<ConvertedVar>) {
    fun toMethod(
        pres: List<Exp>, posts: List<Exp>,
        body: Stmt.Seqn?,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Method =
        method(
            name.asString,
            params.map { it.toLocalVarDecl() },
            returns.map { it.toLocalVarDecl() },
            params.flatMap { it.preconditions() } + pres,
            params.flatMap { it.postconditions() } +
                    returns.flatMap { it.preconditions() } + posts,
            body, pos, info, trafos,
        )
}

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 */
class ProgramConversionContext {
    private val methods: MutableList<Method> = mutableListOf()

    val program: Program
        get() = Program(
            emptySeq(), /* Domains */
            seqOf(field(INT_BACKING_FIELD, Type.Int)), /* Fields */
            emptySeq(), /* Functions */
            emptySeq(), /* Predicates */
            methods.toScalaSeq(), /* Functions */
            emptySeq(), /* Extensions */
            Position.NoPosition.toViper(),
            Info.NoInfo.toViper(),
            Trafos.NoTrafos.toViper()
        )

    fun addWithBody(declaration: FirSimpleFunction) {
        val methodCtx = MethodConversionContext(this, declaration)
        methods.add(methodCtx.fullMethod)
    }

    fun convertType(type: ConeKotlinType): ConvertedOptionalType = when {
        type.isUnit -> ConvertedUnit
        type.isInt -> ConvertedInt
        else -> throw NotImplementedError()
    }
}

/**
 * Contains the metadata for converting a single (specific) Kotlin method;
 * create a new instance for each method if you want to convert multiple.
 * Note that by default we do not convert the whole method: we expect to
 * only need the signature in most methods, as we verify methods one at a
 * time.
 */
class MethodConversionContext(val programCtx: ProgramConversionContext, val declaration: FirSimpleFunction) {
    val fullMethod: Method get() = signature.toMethod(listOf(), listOf(), convertedBody)
    val headerOnlyMethod: Method get() = signature.toMethod(listOf(), listOf(), null)

    val returnVar: ConvertedVar?
    val signature: ConvertedMethodSignature

    init {
        val retType = (declaration.returnTypeRef as FirResolvedTypeRef).type
        val convertedRetType = programCtx.convertType(retType)
        returnVar = (if (convertedRetType is ConvertedType) ConvertedVar(ReturnVariableName, convertedRetType) else null)

        val params = declaration.valueParameters.map {
            ConvertedVar(
                it.convertName(),
                programCtx.convertType((it.returnTypeRef as FirResolvedTypeRef).type) as ConvertedType
            )
        }
        val returns = returnVar?.let { listOf(it) } ?: emptyList()
        signature = ConvertedMethodSignature(declaration.symbol.callableId.convertName(), params, returns)
    }

    private val convertedBody: Stmt.Seqn
        get() {
            val body = declaration.body ?: throw Exception("Functions without a body are not supported yet.")
            val ctx = StmtConversionContext(this)
            ctx.convertAndAppend(body)
            return ctx.block
        }
}

/**
 * Tracks the results of converting a block of statements.
 * Kotlin statements, declarations, and expressions do not map to Viper ones one-to-one.
 * Converting a statement with multiple function calls may require storing the
 * intermediate results, which requires introducing new names.  We thus need a
 * shared context for finding fresh variable names.
 */
class StmtConversionContext(val methodCtx: MethodConversionContext) {
    val statements: MutableList<Stmt> = mutableListOf()
    val declarations: MutableList<Declaration> = mutableListOf()
    val block = Stmt.Seqn(statements, declarations)

    fun convertAndAppend(stmt: FirStatement) {
        stmt.accept(StmtConversionVisitor(), this)
    }
}

/**
 * Convert a statement, emitting the resulting Viper statements and
 * declarations into the context, returning a reference to the
 * expression containing the result.  Note that in the FIR, expressions
 * are a subtype of statements.
 */
class StmtConversionVisitor : FirVisitor<Exp?, StmtConversionContext>() {
    // Note that in some cases we don't expect to ever implement it: we are only
    // translating statements here, after all.  It isn't 100% clear how best to
    // communicate this.
    override fun visitElement(element: FirElement, data: StmtConversionContext): Exp =
        TODO("Not yet implemented for $element (${element.source.text})")

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: StmtConversionContext): Exp? {
        val expr = returnExpression.result.accept(this, data)
        // TODO: respect return-based control flow
        if (expr != null) {
            val returnVar = data.methodCtx.returnVar ?: throw Exception("Expression returned in void function")
            data.statements.add(Stmt.LocalVarAssign(returnVar.toLocalVar(), expr))
        }
        return null
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext): Exp? {
        // TODO: allow blocks to return values
        block.statements.forEach { it.accept(this, data) }
        return null
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: StmtConversionContext): Exp =
        when (constExpression.kind) {
            ConstantValueKind.Int -> Exp.IntLit((constExpression.value as Long).toInt().toScalaBigInt())
            else -> TODO("Not implemented yet")
        }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext,
    ): Exp {
        val symbol = propertyAccessExpression.calleeReference.toResolvedBaseSymbol()!!
        val type = propertyAccessExpression.typeRef.coneTypeOrNull!!
        return when (symbol) {
            is FirValueParameterSymbol -> ConvertedVar(
                symbol.callableId.convertName(),
                data.methodCtx.programCtx.convertType(type) as ConvertedType
            ).toLocalVar()
            else -> TODO("Implement other property accesses")
        }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext): Exp? {
        val id = functionCall.calleeReference.toResolvedCallableSymbol()!!.callableId
        // TODO: figure out a more structured way of doing this
        if (id.packageName.asString() == "kotlin.contracts" && id.callableName.asString() == "contract") return null
        TODO("Implement function call visitation")
    }
}
