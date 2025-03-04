/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import com.intellij.psi.PsiField
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.unexpandedClassId
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.RequiredArgsConstructor
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.psi

@OptIn(DirectDeclarationsAccess::class)
class RequiredArgsConstructorGeneratorPart(session: FirSession) : AbstractConstructorGeneratorPart<RequiredArgsConstructor>(session) {
    override fun getConstructorInfo(classSymbol: FirClassSymbol<*>): RequiredArgsConstructor? {
        return lombokService.getRequiredArgsConstructor(classSymbol)
            ?: lombokService.getData(classSymbol)?.asRequiredArgsConstructor()
    }

    @OptIn(SymbolInternals::class)
    override fun getFieldsForParameters(classSymbol: FirClassSymbol<*>): List<FirJavaField> {
        return classSymbol.fir.declarations
            .filterIsInstance<FirJavaField>()
            .filter { it.isFieldRequired() }
    }

    private fun FirJavaField.isFieldRequired(): Boolean {
        if (isStatic) return false

        // TODO: consider adding `hasInitializer` property directly to java model
        val hasInitializer = (source?.psi as? PsiField)?.hasInitializer() ?: false
        if (hasInitializer) return false
        if (isVal) return true
        return annotations.any { it.unexpandedClassId?.asSingleFqName() in LombokNames.NON_NULL_ANNOTATIONS }
    }
}
