/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacade
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
        LowLevelFirApiFacade.withFirDeclarationOfType<FirValueParameter, KtParameterSymbol>(psi, resolveState) {
            firSymbolBuilder.buildParameterSymbol(it)
        }
    }

    override fun getFunctionSymbol(psi: KtNamedFunction): KtFunctionSymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirSimpleFunction, KtFunctionSymbol>(psi, resolveState) {
            firSymbolBuilder.buildFunctionSymbol(it)
        }
    }

    override fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirConstructor, KtConstructorSymbol>(psi, resolveState) {
            firSymbolBuilder.buildConstructorSymbol(it)
        }
    }

    override fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirTypeParameter, KtTypeParameterSymbol>(psi, resolveState) {
            firSymbolBuilder.buildTypeParameterSymbol(it)
        }
    }

    override fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirTypeAlias, KtTypeAliasSymbol>(psi, resolveState) {
            firSymbolBuilder.buildTypeAliasSymbol(it)
        }
    }

    override fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirEnumEntry, KtEnumEntrySymbol>(psi, resolveState) {
            firSymbolBuilder.buildEnumEntrySymbol(it)
        }
    }

    override fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirSimpleFunction, KtFunctionSymbol>(psi, resolveState) {
            firSymbolBuilder.buildFunctionSymbol(it)
        }
        firSymbolBuilder.buildAnonymousFunctionSymbol(psi.getOrBuildFirOfType(resolveState))
    }

    override fun getAnonymousFunctionSymbol(psi: KtLambdaExpression): KtAnonymousFunctionSymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirAnonymousFunction, KtAnonymousFunctionSymbol>(psi, resolveState) {
            firSymbolBuilder.buildAnonymousFunctionSymbol(it)
        }
    }

    override fun getVariableSymbol(psi: KtProperty): KtVariableSymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirProperty, KtVariableSymbol>(psi, resolveState) {
            firSymbolBuilder.buildVariableSymbol(it)
        }
    }

    override fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirRegularClass, KtClassOrObjectSymbol>(psi, resolveState) {
            firSymbolBuilder.buildClassSymbol(it)
        }
    }

    override fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol = withValidityAssertion {
        LowLevelFirApiFacade.withFirDeclarationOfType<FirPropertyAccessor, KtPropertyAccessorSymbol>(psi, resolveState) {
            firSymbolBuilder.buildPropertyAccessorSymbol(it)
        }
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