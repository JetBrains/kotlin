/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.formver.scala.Option
import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.seqOf
import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp.LocalVar
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp.NeCmp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp.NullLit
import org.jetbrains.kotlin.formver.scala.silicon.ast.Info
import org.jetbrains.kotlin.formver.scala.silicon.ast.Info.NoInfo
import org.jetbrains.kotlin.formver.scala.silicon.ast.Position
import org.jetbrains.kotlin.formver.scala.silicon.ast.Position.NoPosition
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt.Seqn
import org.jetbrains.kotlin.formver.scala.silicon.ast.Trafos
import org.jetbrains.kotlin.formver.scala.silicon.ast.Trafos.NoTrafos
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type.Int
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type.Ref
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import viper.silver.ast.*
import viper.silver.ast.Function

const val INT_BACKING_FIELD = "backing_int"
const val RETURN_VARIABLE_NAME = "ret"

interface ConvertedType {
    val viperType: Type?
    fun preconditions(v: Exp.LocalVar): List<Exp>
    fun postconditions(v: Exp.LocalVar): List<Exp>
}

interface ConvertedNonUnitType : ConvertedType {
    override val viperType: Type
}

abstract class ConvertedPrimitive : ConvertedNonUnitType {
    override fun preconditions(v: Exp.LocalVar): List<Exp> = emptyList()
    override fun postconditions(v: Exp.LocalVar): List<Exp> = emptyList()
}

class ConvertedUnit : ConvertedType {
    override val viperType: Type? = null
    override fun preconditions(v: Exp.LocalVar): List<Exp> = emptyList()
    override fun postconditions(v: Exp.LocalVar): List<Exp> = emptyList()
}

class ConvertedInt : ConvertedPrimitive() {
    override val viperType: Type = Type.Int
}

class ConvertedClassType : ConvertedNonUnitType {
    override val viperType: Type = Type.Ref

    override fun preconditions(v: Exp.LocalVar): List<Exp> = listOf(Exp.NeCmp(v, Exp.NullLit()))
    override fun postconditions(v: Exp.LocalVar): List<Exp> = emptyList()
}

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

// We see a (method) signature as a variable with parameters.
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

class Converter {
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

    fun add(declaration: FirSimpleFunction) {
        methods.add(convertSignature(declaration).toMethod(emptyList(), emptyList(), convertBody(declaration.body!!)))
    }

    private fun convertSignature(declaration: FirSimpleFunction): ConvertedMethodSignature {
        val retType = (declaration.returnTypeRef as FirResolvedTypeRef).type
        val convertedRetType = convertType(retType)
        val params = declaration.valueParameters.map {
            ConvertedVar(
                it.name.toString(),
                convertType((it.returnTypeRef as FirResolvedTypeRef).type) as ConvertedNonUnitType
            )
        }
        val returns = if (convertedRetType is ConvertedNonUnitType) {
            listOf(ConvertedVar(RETURN_VARIABLE_NAME, convertedRetType))
        } else {
            emptyList()
        }
        return ConvertedMethodSignature(declaration.name.asString(), params, returns)
    }

    private fun convertType(type: ConeKotlinType): ConvertedType {
        if (type.isUnit) {
            return ConvertedUnit()
        } else if (type.isInt) {
            return ConvertedInt()
        }
        // Otherwise, still need to get to this case.
        throw NotImplementedError()
    }

    private fun convertBody(body: FirBlock): Seqn {
        val convertedBody = ConvertedBlock(this)
        for (stmt in body.statements) {
            convertedBody.convertAndAppend(stmt)
        }
        return Seqn(convertedBody.statements, convertedBody.declarations)
    }
}

class ConvertedBlock(private val converter: Converter) {
    val statements: MutableList<Stmt> = mutableListOf()
    val declarations: MutableList<Declaration> = mutableListOf()

    fun convertAndAppend(expr: FirExpression) {
        expr.accept(ExpressionConversionVisitor(this))
    }

    fun convertAndAppend(stmt: FirStatement) {
        stmt.accept(ExpressionConversionVisitor(this))
    }
}

class ExpressionConversionVisitor(@Suppress("UNUSED_PARAMETER") block: ConvertedBlock) : FirVisitorVoid() {
    override fun visitElement(element: FirElement) {
        TODO("Not yet implemented")
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression) {
        TODO("not implemented yet")
    }
}
