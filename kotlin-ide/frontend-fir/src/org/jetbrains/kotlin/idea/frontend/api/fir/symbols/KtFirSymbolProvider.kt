/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal class KtFirSymbolProvider(
    override val analysisSession: KtAnalysisSession,
    firSymbolProvider: FirSymbolProvider,
    private val resolveState: FirModuleResolveState,
    private val firSymbolBuilder: KtSymbolByFirBuilder,
    override val token: ValidityToken,
) : KtSymbolProvider(), ValidityTokenOwner {
    private val firSymbolProvider by weakRef(firSymbolProvider)

    override fun getParameterSymbol(psi: KtParameter): KtParameterSymbol = withValidityAssertion {
        firSymbolBuilder.buildParameterSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getFunctionSymbol(psi: KtNamedFunction): KtFunctionSymbol = withValidityAssertion {
        firSymbolBuilder.buildFunctionSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol = withValidityAssertion {
        firSymbolBuilder.buildConstructorSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol = withValidityAssertion {
        firSymbolBuilder.buildTypeParameterSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol = withValidityAssertion {
        firSymbolBuilder.buildTypeAliasSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol = withValidityAssertion {
        firSymbolBuilder.buildEnumEntrySymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol = withValidityAssertion {
        firSymbolBuilder.buildAnonymousFunctionSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getAnonymousFunctionSymbol(psi: KtLambdaExpression): KtAnonymousFunctionSymbol = withValidityAssertion {
        firSymbolBuilder.buildAnonymousFunctionSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getVariableSymbol(psi: KtProperty): KtVariableSymbol = withValidityAssertion {
        firSymbolBuilder.buildVariableSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol = withValidityAssertion {
        firSymbolBuilder.buildClassSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol = withValidityAssertion {
        firSymbolBuilder.buildPropertyAccessorSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol? = withValidityAssertion {
        val symbol = firSymbolProvider.getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol ?: return null
        firSymbolBuilder.buildClassSymbol(symbol.fir)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtSymbol> {
        val firs = firSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        return firs.asSequence().map { firSymbol -> firSymbolBuilder.buildSymbol(firSymbol.fir) }
    }
}