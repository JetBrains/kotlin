/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.*
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*
import org.jetbrains.sir.lightclasses.utils.isSubtypeOf
import org.jetbrains.sir.lightclasses.utils.overridableCandidates
import org.jetbrains.sir.lightclasses.utils.translateParameters
import org.jetbrains.sir.lightclasses.utils.translateReturnType

internal class SirFunctionFromKtSymbol(
    override val ktSymbol: KaNamedFunctionSymbol,
    override val ktModule: KaModule,
    override val sirSession: SirSession,
) : SirFunction(), SirFromKtSymbol<KaNamedFunctionSymbol> {

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val name: String by lazyWithSessions {
        ktSymbol.sirDeclarationName()
    }
    override val extensionReceiverParameter: SirParameter? by lazy {
        translateExtensionParameter()
    }
    override val parameters: List<SirParameter> by lazy {
        translateParameters()
    }
    override val returnType: SirType by lazy {
        translateReturnType()
    }
    override val documentation: String? by lazyWithSessions {
        ktSymbol.documentation()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(useSiteSession)
        }
        set(_) = Unit

    override val isOverride: Boolean get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirFunction>? by lazy { computeIsOverride() }

    override val isInstance: Boolean
        get() = !ktSymbol.isTopLevel && !ktSymbol.isStatic

    override val modality: SirModality
        get() = ktSymbol.modality.sirModality

    override val attributes: List<SirAttribute> by lazy {
        this.translatedAttributes + listOfNotNull(SirAttribute.NonOverride.takeIf { overrideStatus is OverrideStatus.Conflicts })
    }

    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never

    override var body: SirFunctionBody? = null
}
