/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import com.intellij.psi.PsiField
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.AllArgsConstructor
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class AllArgsConstructorGeneratorPart(session: FirSession) : AbstractConstructorGeneratorPart<AllArgsConstructor>(session) {
    override fun getConstructorInfo(classSymbol: FirClassSymbol<*>): AllArgsConstructor? {
        return lombokService.getAllArgsConstructor(classSymbol)
            ?: runIf(!containsExplicitConstructor(classSymbol)) {
                lombokService.getBuilder(classSymbol)?.let { AllArgsConstructor(JavaVisibilities.PackageVisibility) }
                    ?: lombokService.getValue(classSymbol)?.asAllArgsConstructor()
            }
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    override fun getFieldsForParameters(classSymbol: FirClassSymbol<*>): List<FirJavaField> {
        val isAllArgsConstructor = lombokService.getAllArgsConstructor(classSymbol) != null

        return buildList {
            for (declaration in classSymbol.fir.declarations) {
                if (declaration !is FirJavaField || declaration.isStatic) continue

                // TODO: consider adding `hasInitializer` property directly to java model
                val hasInitializer = (declaration.source?.psi as? PsiField)?.hasInitializer() ?: false

                if (hasInitializer && (!isAllArgsConstructor || !declaration.isVar)) continue

                add(declaration)
            }
        }
    }
}
