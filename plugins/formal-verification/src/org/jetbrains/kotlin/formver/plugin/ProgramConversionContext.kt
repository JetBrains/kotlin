/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.seqOf
import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Info
import org.jetbrains.kotlin.formver.scala.silicon.ast.Position
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt.Seqn
import org.jetbrains.kotlin.formver.scala.silicon.ast.Trafos
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type
import org.jetbrains.kotlin.formver.scala.toScalaBigInt
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.ConstantValueKind
import viper.silver.ast.*

const val INT_BACKING_FIELD = "backing_int"
const val RETURN_VARIABLE_NAME = "ret"

class ConvertedVar(val name: String, val type: ConvertedNonUnitType) {
    fun toLocalVarDecl(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): LocalVarDecl = localVarDecl(name, type.viperType, pos, info, trafos)

    fun toLocalVar(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.LocalVar = Exp.LocalVar(name, type.viperType, pos, info, trafos)

    fun preconditions(): List<Exp> = type.preconditions(toLocalVar())
    fun postconditions(): List<Exp> = type.postconditions(toLocalVar())
}

class ConvertedMethodSignature(val name: String, val params: List<ConvertedVar>, val returns: List<ConvertedVar>) {
    fun toMethod(
        pres: List<Exp>, posts: List<Exp>,
        body: Seqn?,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Method =
        method(
            name,
            params.map { it.toLocalVarDecl() },
            returns.map { it.toLocalVarDecl() },
            params.flatMap { it.preconditions() } + pres,
            params.flatMap { it.postconditions() } +
                    returns.flatMap { it.preconditions() } + posts,
            body, pos, info, trafos,
        )
}

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
        val methodCtx = MethodConversionContext(this, declaration);
        methods.add(methodCtx.fullMethod)
    }

    fun convertType(type: ConeKotlinType): ConvertedType {
        if (type.isUnit) {
            return ConvertedUnit()
        } else if (type.isInt) {
            return ConvertedInt()
        }
        // Otherwise, still need to get to this case.
        throw NotImplementedError()
    }
}

class MethodConversionContext(val programCtx: ProgramConversionContext, val declaration: FirSimpleFunction) {
    val fullMethod: Method get() = signature.toMethod(listOf(), listOf(), convertedBody)
    val headerOnlyMethod: Method get() = signature.toMethod(listOf(), listOf(), null)

    val returnVar: ConvertedVar?
    val signature: ConvertedMethodSignature

    init {
        val retType = (declaration.returnTypeRef as FirResolvedTypeRef).type
        val convertedRetType = programCtx.convertType(retType)
        returnVar = (if (convertedRetType is ConvertedNonUnitType) ConvertedVar(RETURN_VARIABLE_NAME, convertedRetType) else null)

        val params = declaration.valueParameters.map {
            ConvertedVar(
                it.name.toString(),
                programCtx.convertType((it.returnTypeRef as FirResolvedTypeRef).type) as ConvertedNonUnitType
            )
        }
        val returns = returnVar?.let { listOf(it) } ?: emptyList()
        signature = ConvertedMethodSignature(declaration.name.asString(), params, returns)
    }

    private val convertedBody: Seqn
        get() {
            val body = declaration.body ?: throw Exception("Functions without a body are not supported yet.")
            val ctx = StmtConversionContext(this)
            ctx.convertAndAppend(body)
            return ctx.block
        }
}

class StmtConversionContext(val methodCtx: MethodConversionContext) {
    val statements: MutableList<Stmt> = mutableListOf()
    val declarations: MutableList<Declaration> = mutableListOf()
    val block = Seqn(statements, declarations)

    fun convertAndAppend(stmt: FirStatement) {
        stmt.accept(StmtConversionVisitor(), this)
    }
}

class StmtConversionVisitor : FirVisitor<Exp?, StmtConversionContext>() {
    override fun visitElement(element: FirElement, data: StmtConversionContext): Exp? {
        throw Exception("StmtConversionVisitor should only be used to convert statements.")
    }

    override fun visitStatement(statement: FirStatement, data: StmtConversionContext): Exp? {
        TODO("Not yet implemented for $statement (${statement.source.text})")
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: StmtConversionContext): Exp? {
        val expr = returnExpression.result.accept(this, data)
        // TODO: respect return-based control flow
        if (expr != null) {
            val returnVar = data.methodCtx.returnVar ?: throw Exception("Expression returned in void function")
            data.statements.add(Stmt.LocalVarAssign(returnVar.toLocalVar(), expr))
        }
        System.err.println("Visiting: $returnExpression")
        return null
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext): Exp? {
        // TODO: allow blocks to return values
        block.statements.forEach { it.accept(this, data) }
        return null
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: StmtConversionContext): Exp? {
        when (constExpression.kind) {
            ConstantValueKind.Int -> return Exp.IntLit((constExpression.value as Long).toInt().toScalaBigInt())
            else -> TODO("Not implemented yet")
        }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext,
    ): Exp? {
        System.err.println("Visiting: $propertyAccessExpression")
        return null
    }
}
