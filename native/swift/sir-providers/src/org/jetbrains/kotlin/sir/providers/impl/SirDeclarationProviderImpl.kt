/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.*
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.kotlin.sir.util.addChild

public class SirDeclarationProviderImpl(
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirDeclarationProvider {

    private val visitedDeclarations: MutableMap<KtDeclarationSymbol, SirDeclaration> = mutableMapOf()

    override fun KtDeclarationSymbol.sirDeclaration(): SirDeclaration = withSirAnalyse(sirSession, ktAnalysisSession) {
        val parent = getSirParent() as? SirMutableDeclarationContainer
            ?: throw IllegalStateException(
                "Encountered parent that is not mutable declaration - ${getSirParent()}. " +
                        "Cannot add child - abort transforming symbol ${this@sirDeclaration}"
            )
        visitedDeclarations.getOrPut(this@sirDeclaration) {
            parent.addChild {
                val res = when (val ktSymbol = this@sirDeclaration) {
                    is KtNamedClassOrObjectSymbol -> {
                        ktSymbol.sirClass()
                    }
                    is KtConstructorSymbol -> {
                        ktSymbol.sirInit()
                    }
                    is KtFunctionLikeSymbol -> {
                        ktSymbol.sirFunction()
                    }
                    is KtVariableSymbol -> {
                        ktSymbol.sirVariable()
                    }
                    else -> TODO("encountered unknown symbol type - $ktSymbol. Error system should be reworked KT-65980")
                }
                visitedDeclarations[this@sirDeclaration] = res
                res
            }
                .also {
                    // hack: to touch all children only after the class is already cached.
                    // Will be removed for lazy implementation, can also be solved by using some kind of deferred/promise/future implementation
                    if (this@sirDeclaration is KtNamedClassOrObjectSymbol) {
                        val symbol = this@sirDeclaration
                        symbol.getCombinedDeclaredMemberScope().extractDeclarations()
                            .toList() // without `toList` actually prints nothing.
                    }
                }
        }
    }

    context(KtAnalysisSession, SirSession)
    private fun KtNamedClassOrObjectSymbol.sirClass(): SirNamedDeclaration = buildClass {
        val symbol = this@sirClass
        name = symbol.name.asString()
        origin = KotlinSource(symbol)

        documentation = symbol.documentation()
    }

    context(KtAnalysisSession, SirSession)
    private fun KtFunctionLikeSymbol.sirFunction(): SirFunction = buildFunction {
        val symbol = this@sirFunction
        origin = KotlinSource(symbol)

        kind = symbol.sirCallableKind

        name = symbol.sirDeclarationName()

        symbol.valueParameters.mapTo(parameters) {
            SirParameter(
                argumentName = it.name.asString(),
                type = it.returnType.translateType()
            )
        }
        returnType = symbol.returnType.translateType()
        documentation = symbol.documentation()
    }

    context(KtAnalysisSession, SirSession)
    private fun KtConstructorSymbol.sirInit(): SirInit = buildInit {
        val symbol = this@sirInit
        origin = KotlinSource(symbol)

        kind = symbol.sirCallableKind
        isFailable = false
        initKind = SirInitializerKind.ORDINARY

        symbol.valueParameters.mapTo(parameters) {
            SirParameter(
                argumentName = it.name.asString(),
                type = it.returnType.translateType()
            )
        }

        documentation = symbol.documentation()
    }

    context(KtAnalysisSession, SirSession)
    private fun KtVariableSymbol.sirVariable(): SirVariable = buildVariable {
        val symbol = this@sirVariable
        origin = KotlinSource(symbol)

        name = symbol.sirDeclarationName()
        type = symbol.returnType.translateType()

        val accessorKind = symbol.sirCallableKind
        getter = buildGetter {
            kind = accessorKind
        }
        setter = if (!symbol.isVal) buildSetter {
            kind = accessorKind
        } else null

        documentation = symbol.documentation()
    }.also {
        it.getter.parent = it
        it.setter?.parent = it
    }

    private fun KtSymbol.documentation(): String? = this.psiSafe<KtDeclaration>()?.docComment?.text

    private val KtCallableSymbol.sirCallableKind: SirCallableKind
        get() = when (symbolKind) {
            KtSymbolKind.TOP_LEVEL -> {
                val isRootPackage = callableIdIfNonLocal?.packageName?.isRoot
                if (isRootPackage == true) {
                    SirCallableKind.FUNCTION
                } else {
                    SirCallableKind.STATIC_METHOD
                }
            }
            KtSymbolKind.CLASS_MEMBER, KtSymbolKind.ACCESSOR,
            -> SirCallableKind.INSTANCE_METHOD
            KtSymbolKind.LOCAL,
            KtSymbolKind.SAM_CONSTRUCTOR,
            -> TODO("encountered callable kind($symbolKind) that is not translatable currently. Fix this crash during KT-65980.")
        }
}
