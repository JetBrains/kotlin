/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtPackageScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KtFirPackageScope(
    firScope: FirPackageMemberScope,
    private val builder: KtSymbolByFirBuilder,
    override val token: ValidityToken,
) : KtPackageScope, ValidityTokenOwner {
    private val firScope by weakRef(firScope)
    override val fqName: FqName get() = firScope.fqName
    private val provider get() = firScope.session.firProvider

    override fun getCallableNames(): Set<Name> = withValidityAssertion {
        provider.getAllCallableNamesInPackage(fqName)
    }

    override fun getClassLikeSymbolNames(): Set<Name> = withValidityAssertion {
        provider.getClassNamesInPackage(fqName)
    }

    override fun getCallableSymbols(): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getCallableNames(), builder)
    }

    override fun getClassClassLikeSymbols(): Sequence<KtClassLikeSymbol> = withValidityAssertion {
        firScope.getClassLikeSymbols(getClassLikeSymbolNames(), builder)
    }
}