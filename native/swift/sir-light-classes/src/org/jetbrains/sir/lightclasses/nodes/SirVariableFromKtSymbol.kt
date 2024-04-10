/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildSetter
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.sirCallableKind

internal class SirVariableFromKtSymbol(
    override val ktSymbol: KtVariableSymbol,
    override val analysisApiSession: KtAnalysisSession,
    override val sirSession: SirSession,
) : SirVariable(), SirFromKtSymbol {

    override val visibility: SirVisibility = SirVisibility.PUBLIC

    override val origin: SirOrigin by lazyWithSessions {
        KotlinSource(ktSymbol)
    }
    override val name: String by lazyWithSessions {
        ktSymbol.sirDeclarationName()
    }
    override val type: SirType by lazyWithSessions {
        ktSymbol.returnType.translateType()
    }
    override val getter: SirGetter by lazyWithSessions {
        buildGetter {
            kind = accessorKind
        }.also {
            it.parent = this@SirVariableFromKtSymbol
        }
    }
    override val setter: SirSetter? by lazyWithSessions {
        if (!ktSymbol.isVal) {
            buildSetter {
                kind = accessorKind
            }.also {
                it.parent = this@SirVariableFromKtSymbol
            }
        } else {
            null
        }
    }
    override val documentation: String? by lazyWithSessions {
        ktSymbol.documentation()
    }

    override var parent: SirDeclarationParent
        get() = withSirAnalyse(sirSession, analysisApiSession) {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    private val accessorKind by lazyWithSessions {
        ktSymbol.sirCallableKind
    }
}
