/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.kt.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.kt.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.kt.KotlinSource
import org.jetbrains.kotlin.sir.kt.SirSession
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

public class SirClassFromSymbol(
    private val ktSymbol: KtNamedClassOrObjectSymbol,
    public override val ktAnalysisSession: KtAnalysisSession,
    public override val sirSession: SirSession,
) : SirClass(), SirDeclarationFromSymbol {
    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val visibility: SirVisibility by lazy {
        withTranslationContext {
            ktSymbol.sirVisibility()!!
        }
    }
    override var parent: SirDeclarationParent
        get() {
            return withTranslationContext {
                ktSymbol.sirDeclarationParent()
            }
        }
        set(value) {}
    override val name: String by lazy {
        withTranslationContext {
            ktSymbol.sirDeclarationName()
        }
    }

    override val superClass: SirType? by lazy {
        withTranslationContext {
            if (ktSymbol.buildSelfClassType().isSubTypeOf(builtinTypes.ANY)) {
                SirPredefinedNominalType(KotlinRuntimeModule.kotlinBase)
            } else {
                null
            }
        }
    }

    override val declarations: List<SirDeclaration> by lazy {
        withTranslationContext {
            val memberScope = ktSymbol.getDeclaredMemberScope().getCallableSymbols().mapNotNull {
                when (it) {
                    is KtKotlinPropertySymbol -> {
                        if (ktSymbol.sirVisibility() == null) null
                        else SirVariableFromSymbol(it, ktAnalysisSession, sirSession)
                    }
                    is KtFunctionLikeSymbol -> {
                        if (ktSymbol.sirVisibility() == null) null
                        else SirFunctionFromSymbol(it, ktAnalysisSession, sirSession)
                    }
                    else -> {
                        null
                    }
                }
            }
            val nestedDeclarations = ktSymbol.getStaticDeclaredMemberScope().getClassifierSymbols().mapNotNull {
                when (it) {
                    is KtNamedClassOrObjectSymbol -> {
                        if (ktSymbol.sirVisibility() == null) null
                        else SirClassFromSymbol(it, ktAnalysisSession, sirSession)
                    }
                    else -> null
                }
            }
            (nestedDeclarations + memberScope).toList()

        }
    }

    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {}
}