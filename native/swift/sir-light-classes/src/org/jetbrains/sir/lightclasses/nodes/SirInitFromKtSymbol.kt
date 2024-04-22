/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.computeIsOverrideForDesignatedInit
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.sirCallableKind

internal class SirInitFromKtSymbol(
    override val ktSymbol: KtConstructorSymbol,
    override val analysisApiSession: KtAnalysisSession,
    override val sirSession: SirSession,
) : SirInit(), SirFromKtSymbol {

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val isFailable: Boolean = false
    override val initKind: SirInitializerKind = SirInitializerKind.ORDINARY

    override val origin: SirOrigin by lazyWithSessions {
        KotlinSource(ktSymbol)
    }
    override val kind: SirCallableKind by lazyWithSessions {
        SirCallableKind.CLASS_METHOD
    }
    override val parameters: MutableList<SirParameter> by lazyWithSessions {
        mutableListOf<SirParameter>().apply {
            ktSymbol.valueParameters.mapTo(this) {
                SirParameter(argumentName = it.name.asString(), type = it.returnType.translateType())
            }
        }
    }
    override val documentation: String? by lazyWithSessions {
        ktSymbol.documentation()
    }

    override val isOverride: Boolean by lazy {
        computeIsOverrideForDesignatedInit(parent, parameters)
    }

    override var parent: SirDeclarationParent
        get() = withSirAnalyse(sirSession, analysisApiSession) {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    override var body: SirFunctionBody? = null
}