/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.OverrideStatus
import org.jetbrains.sir.lightclasses.utils.computeIsOverride
import org.jetbrains.sir.lightclasses.utils.translateParameters
import org.jetbrains.sir.lightclasses.utils.translatedAttributes

internal class SirInitFromKtSymbol(
    override val ktSymbol: KaConstructorSymbol,
    override val ktModule: KaModule,
    override val sirSession: SirSession,
) : SirInit(), SirFromKtSymbol<KaConstructorSymbol> {

    override val visibility: SirVisibility by lazyWithSessions {
        ktSymbol.sirVisibility(this) ?: error("$ktSymbol shouldn't be exposed to SIR")
    }

    override val isFailable: Boolean = false

    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val parameters: List<SirParameter> by lazy {
        translateParameters()
    }
    override val documentation: String? by lazyWithSessions {
        ktSymbol.documentation()
    }

    override val isRequired: Boolean = false

    override val isConvenience: Boolean = false

    override val isOverride: Boolean get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirInit>? by lazy { computeIsOverride() }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(useSiteSession)
        }
        set(_) = Unit

    override val attributes: List<SirAttribute> by lazy {
        this.translatedAttributes + listOfNotNull(SirAttribute.NonOverride.takeIf { overrideStatus is OverrideStatus.Conflicts })
    }

    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never

    override var body: SirFunctionBody? = null
}