/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.unexpandedClassId
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.RequiredArgsConstructor
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@OptIn(DirectDeclarationsAccess::class)
class RequiredArgsConstructorGeneratorPart(session: FirSession) : AbstractConstructorGeneratorPart<RequiredArgsConstructor>(session) {
    override fun getConstructorInfo(classSymbol: FirClassSymbol<*>): RequiredArgsConstructor? {
        if (!classSymbol.hasJavaOrigin) return getKotlinConstructorInfo(classSymbol)

        return lombokService.getRequiredArgsConstructor(classSymbol)
            ?: runIf(!containsExplicitConstructor(classSymbol)) { getDataAsRequiredArgsConstructor(classSymbol) }
    }

    /**
     * A Kotlin class always keeps the constructor written for it, so the only thing `@Data` can contribute is its
     * `staticConstructor`: a factory in the companion object that takes all the primary constructor parameters and
     * passes them on to it. A constructor of its own would leave the properties of the primary one uninitialized.
     *
     * Without a primary constructor there is nothing for the factory to delegate to, and a vararg parameter is skipped
     * by the clash checks, which would let a duplicate of the primary constructor through.
     */
    private fun getKotlinConstructorInfo(classSymbol: FirClassSymbol<*>): RequiredArgsConstructor? {
        if (classSymbol.primaryConstructorParameters() == null) return null

        return lombokService.getRequiredArgsConstructor(classSymbol)
            ?: getDataAsRequiredArgsConstructor(classSymbol)?.takeIf { it.staticName != null }
    }

    private fun getDataAsRequiredArgsConstructor(classSymbol: FirClassSymbol<*>): RequiredArgsConstructor? {
        return lombokService.getData(classSymbol)?.takeIf {
            lombokService.getAllArgsConstructor(classSymbol) == null &&
                    lombokService.getNoArgsConstructor(classSymbol) == null
        }?.asRequiredArgsConstructor()
    }

    /**
     * The required fields for a Java class, and all the primary constructor parameters for a Kotlin one: these are
     * exactly what the primary constructor called by the generated factory needs, see [getKotlinConstructorInfo].
     */
    @OptIn(SymbolInternals::class)
    override fun getVariablesForParameters(classSymbol: FirClassSymbol<*>): List<FirVariable> {
        if (!classSymbol.hasJavaOrigin) return classSymbol.primaryConstructorParameters().orEmpty()

        return classSymbol.fir.declarations
            .filterIsInstance<FirJavaField>()
            .filter { it.isFieldRequired() }
    }

    private fun FirJavaField.isFieldRequired(): Boolean {
        if (isStatic) return false
        if (hasInitializer) return false
        if (isVal) return true
        return annotations.any { it.unexpandedClassId?.asSingleFqName() in LombokNames.NON_NULL_ANNOTATIONS }
    }

    @OptIn(SymbolInternals::class)
    private fun FirClassSymbol<*>.primaryConstructorParameters(): List<FirValueParameter>? {
        val primaryConstructor = fir.declarations.filterIsInstance<FirConstructor>().firstOrNull { it.isPrimary } ?: return null
        return primaryConstructor.valueParameters.takeIf { parameters -> parameters.none { it.isVararg } }
    }
}
