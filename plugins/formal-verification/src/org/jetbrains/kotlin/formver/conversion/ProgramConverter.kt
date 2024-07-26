/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.seqOf
import org.jetbrains.kotlin.formver.scala.silicon.ast.Info
import org.jetbrains.kotlin.formver.scala.silicon.ast.Position
import org.jetbrains.kotlin.formver.scala.silicon.ast.Trafos
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import viper.silver.ast.Program

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(val session: FirSession) : ProgramConversionContext {
    private val methods: MutableMap<ConvertedName, MethodConverter> = mutableMapOf()

    val program: Program
        get() = Program(
            seqOf(UnitDomain.toViper(), NullableDomain.toViper()), /* Domains */
            seqOf(), /* Fields */
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
        val contractDescription = declaration.contractDescription as? FirResolvedContractDescription
        // NOTE: we have a problem here if we initially specify a method without a body,
        // and then later decide to add a body anyway.  It's not a problem for now, but
        // worth being aware of.
        methods.getOrPut(signature.name) {
            MethodConverter(this, signature, declaration.body, contractDescription)
        }
    }

    override fun add(symbol: FirNamedFunctionSymbol): ConvertedMethodSignature {
        val signature = convertSignature(symbol)
        val contractDescription = symbol.resolvedContractDescription
        methods.getOrPut(signature.name) {
            MethodConverter(this, signature, null, contractDescription)
        }
        return signature
    }

    private fun convertSignature(symbol: FirNamedFunctionSymbol): ConvertedMethodSignature {
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

    override fun convertType(type: ConeKotlinType): ConvertedType = when {
        type.isUnit -> ConvertedUnit
        type.isInt -> ConvertedInt
        type.isBoolean -> ConvertedBoolean
        type.isNothing -> ConvertedNothing
        type.isNullable -> ConvertedNullable(convertType(type.withNullability(ConeNullability.NOT_NULL, session.typeContext)))
        else -> throw NotImplementedError("The embedding for type $type is not yet implemented.")
    }
}

