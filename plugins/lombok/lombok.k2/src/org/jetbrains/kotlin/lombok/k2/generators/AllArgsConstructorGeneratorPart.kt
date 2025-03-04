/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import com.intellij.psi.PsiField
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.AllArgsConstructor
import org.jetbrains.kotlin.psi

class AllArgsConstructorGeneratorPart(session: FirSession) : AbstractConstructorGeneratorPart<AllArgsConstructor>(session) {
    override fun getConstructorInfo(classSymbol: FirClassSymbol<*>): AllArgsConstructor? {
        return lombokService.getAllArgsConstructor(classSymbol)
            ?: lombokService.getValue(classSymbol)?.asAllArgsConstructor()
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    override fun getFieldsForParameters(classSymbol: FirClassSymbol<*>): List<FirJavaField> {
        return classSymbol.fir.declarations
            .filterIsInstance<FirJavaField>()
            .filter { it.isFieldAllowed() }
    }

    private fun FirJavaField.isFieldAllowed(): Boolean {
        if (isStatic) return false

        // TODO: consider adding `hasInitializer` property directly to java model
        val hasInitializer = (source?.psi as? PsiField)?.hasInitializer() ?: false
        return isVar || !hasInitializer
    }
}
