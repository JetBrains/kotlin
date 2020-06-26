/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwner
import org.jetbrains.kotlin.idea.frontend.api.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal class KtFirConstructorValueParameterSymbol(
    fir: FirValueParameterImpl,
    override val token: ValidityOwner,
    private val builder: KtSymbolByFirBuilder
) : KtConstructorParameterSymbol(), KtFirSymbol<FirValueParameterImpl> {
    override val fir: FirValueParameterImpl by weakRef(fir)
    override val psi: PsiElement? by cached { fir.findPsi(fir.session) }

    override val name: Name get() = withValidityAssertion { fir.name }
    override val type: KtType by cached { builder.buildKtType(fir.returnTypeRef) }
    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion {
            when {
                fir.isVal || fir.isVal -> KtSymbolKind.MEMBER
                else -> KtSymbolKind.LOCAL
            }
        }
    override val constructorParameterKind: KtConstructorParameterSymbolKind
        get() = withValidityAssertion {
            when {
                fir.isVal -> KtConstructorParameterSymbolKind.VAL_PROPERTY
                fir.isVar -> KtConstructorParameterSymbolKind.VAR_PROPERTY
                else -> KtConstructorParameterSymbolKind.NON_PROPERTY
            }
        }
}