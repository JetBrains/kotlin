/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.seqOf
import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import org.jetbrains.kotlin.utils.addToStdlib.getOrPut
import viper.silver.ast.Method
import viper.silver.ast.Program

const val INT_BACKING_FIELD = "backing_int"

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 */
class ProgramConversionContext {
    private val methods: MutableMap<ConvertedName, MethodConversionContext> = mutableMapOf()

    val program: Program
        get() = Program(
            seqOf(UnitDomain.toViper()), /* Domains */
            seqOf(field(INT_BACKING_FIELD, Type.Int)), /* Fields */
            emptySeq(), /* Functions */
            emptySeq(), /* Predicates */
            methods.values.map { it.toMethod }.toList().toScalaSeq(), /* Functions */
            emptySeq(), /* Extensions */
            Position.NoPosition.toViper(),
            Info.NoInfo.toViper(),
            Trafos.NoTrafos.toViper()
        )

    fun addWithBody(declaration: FirSimpleFunction) {
        val signature = convertSignature(declaration.symbol)
        // NOTE: we have a problem here if we initially specify a method without a body,
        // and then later decide to add a body anyway.  It's not a problem for now, but
        // worth being aware of.
        methods.getOrPut(signature.name) {
            MethodConversionContext(this, signature, declaration.body)
        }
    }

    fun add(symbol: FirNamedFunctionSymbol): ConvertedMethodSignature {
        val signature = convertSignature(symbol)
        methods.getOrPut(signature.name) {
            MethodConversionContext(this, signature, null)
        }
        return signature
    }

    fun convertSignature(symbol: FirNamedFunctionSymbol): ConvertedMethodSignature {
        val retType = symbol.resolvedReturnTypeRef.type

        val params = symbol.valueParameterSymbols.map {
            ConvertedVar(it.convertName(), convertType(it.resolvedReturnType))
        }
        return ConvertedMethodSignature(
            symbol.callableId.convertName(),
            params,
            convertType(retType)
        )
    }

    fun convertType(type: ConeKotlinType): ConvertedType = when {
        type.isUnit -> ConvertedUnit
        type.isInt -> ConvertedInt
        type.isBoolean -> ConvertedBoolean
        else -> throw NotImplementedError("The embedding for type $type is not yet implemented.")
    }
}

