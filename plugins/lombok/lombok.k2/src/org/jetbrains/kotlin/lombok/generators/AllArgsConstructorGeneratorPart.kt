/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.AllArgsConstructor

class AllArgsConstructorGeneratorPart(session: FirSession) : AbstractConstructorGeneratorPart<AllArgsConstructor>(session) {
    override fun getConstructorInfo(classSymbol: FirClassSymbol<*>): AllArgsConstructor? {
        return lombokService.getAllArgsConstructor(classSymbol) ?: run {
            val builderAllArgs = lombokService.getBuilder(classSymbol)?.asAllArgsConstructor(classSymbol)
            val valueAllArgs = lombokService.getValue(classSymbol)?.asAllArgsConstructor(classSymbol)
            when {
                builderAllArgs == null -> valueAllArgs
                valueAllArgs == null -> builderAllArgs
                // If we have both annotations, we should generate AllArgsConstructor with PACKAGE access (as per @Builder),
                // but choosing arguments per @Value rules (so, skipping everything initialized)
                else -> AllArgsConstructor(accessLevel = AccessLevel.PACKAGE, annotation = valueAllArgs.annotation)
            }
        }
    }

    override fun getVariablesForParameters(classSymbol: FirClassSymbol<*>): List<FirVariable> {
        return getConstructorInfo(classSymbol)?.getVariablesForParameters(classSymbol).orEmpty()
    }
}
