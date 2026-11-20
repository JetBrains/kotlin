/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.RequiredArgsConstructor

class RequiredArgsConstructorGeneratorPart(session: FirSession) : AbstractConstructorGeneratorPart<RequiredArgsConstructor>(session) {
    override fun getConstructorInfo(classSymbol: FirClassSymbol<*>): RequiredArgsConstructor? {
        if (!classSymbol.hasJavaOrigin) return getKotlinConstructorInfo(classSymbol)

        return lombokService.getRequiredArgsConstructor(classSymbol)
            ?: getDataAsRequiredArgsConstructor(classSymbol)
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
        }?.asRequiredArgsConstructor(classSymbol)
    }

    /**
     * The required fields for a Java class, and all the primary constructor parameters for a Kotlin one: these are
     * exactly what the primary constructor called by the generated factory needs, see [getKotlinConstructorInfo].
     */
    override fun getVariablesForParameters(classSymbol: FirClassSymbol<*>): List<FirVariable> {
        return getConstructorInfo(classSymbol)?.getVariablesForParameters(classSymbol).orEmpty()
    }
}
