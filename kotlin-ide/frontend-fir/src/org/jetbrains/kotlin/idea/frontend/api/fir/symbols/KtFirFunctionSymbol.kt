/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCommonSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KtFirFunctionSymbol(
    fir: FirSimpleFunction,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
    private val forcedOrigin: FirDeclarationOrigin? = null
) : KtFunctionSymbol(), KtFirSymbol<FirSimpleFunction> {
    override val fir: FirSimpleFunction by weakRef(fir)
    override val psi: PsiElement? by cached { fir.findPsi(fir.session) }
    override val name: Name get() = withValidityAssertion { fir.name }
    override val type: KtType by cached { builder.buildKtType(fir.returnTypeRef) }
    override val valueParameters: List<KtFirFunctionValueParameterSymbol> by cached {
        fir.valueParameters.map { valueParameter ->
            check(valueParameter is FirValueParameterImpl)
            builder.buildParameterSymbol(valueParameter)
        }
    }
    override val typeParameters by cached {
        fir.typeParameters.map { typeParameter ->
            builder.buildTypeParameterSymbol(typeParameter.symbol.fir)
        }
    }
    override val origin: KtSymbolOrigin get() = withValidityAssertion { forcedOrigin?.asKtSymbolOrigin() ?: super.origin }
    override val isSuspend: Boolean get() = withValidityAssertion { fir.isSuspend }
    override val receiverType: KtType? by cached { fir.receiverTypeRef?.let(builder::buildKtType) }
    override val isOperator: Boolean get() = withValidityAssertion { fir.isOperator }
    override val isExtension: Boolean get() = withValidityAssertion { fir.receiverTypeRef != null }
    override val fqName: FqName? get() = withValidityAssertion { fir.symbol.callableId.asFqNameForDebugInfo() }
    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion {
            when {
                fir.isLocal -> KtSymbolKind.LOCAL
                fir.symbol.callableId.classId == null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.MEMBER
            }
        }
    override val modality: KtCommonSymbolModality get() = withValidityAssertion { fir.modality.getSymbolModality() }
}