/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.AllArgsConstructor

class AllArgsConstructorGeneratorPart(session: FirSession) : AbstractConstructorGeneratorPart<AllArgsConstructor>(session) {
    override fun getConstructorInfo(classSymbol: FirClassSymbol<*>): AllArgsConstructor? {
        return lombokService.getAllArgsConstructor(classSymbol)
            ?: lombokService.getBuilder(classSymbol)?.asAllArgsConstructor(classSymbol)
            ?: lombokService.getValue(classSymbol)?.asAllArgsConstructor(classSymbol)
    }

    override fun getVariablesForParameters(classSymbol: FirClassSymbol<*>): List<FirVariable> {
        return getConstructorInfo(classSymbol)?.getVariablesForParameters(classSymbol).orEmpty()
    }
}
