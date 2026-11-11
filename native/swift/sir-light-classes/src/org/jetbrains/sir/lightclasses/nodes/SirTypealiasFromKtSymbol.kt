/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.containingModule
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.translateType
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*

internal class SirTypealiasFromKtSymbol(
    override val ktSymbol: KaTypeAliasSymbol,
    override val sirSession: SirSession,
) : SirTypealias(), SirFromKtSymbol<KaTypeAliasSymbol> {

    override val origin: SirOrigin = KotlinSource(ktSymbol)
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    private val kdocElements: KDocElements? by lazyWithSessions {
        KDocElements(this)
    }
    override val documentation: String? by lazyWithSessions {
        translateDocumentation(kdocElements)
    }
    override val name: String by lazy {
        ktSymbol.name.asString()
    }

    private val translatedTypeParams: TranslatedTypeParameters by lazyWithSessions {
        translateTypeParametersAndConstraints(
            declarationSymbol = ktSymbol,
            ownKtTypeParams = ktSymbol.typeParameters,
            enclosingScope = enclosingTypeParameterScope(),
            expandedType = ktSymbol.expandedType,
        )
    }

    override val typeParameters: List<SirTypeParameter>
        get() = translatedTypeParams.typeParameters

    override val constraints: List<SirTypeConstraint>
        get() = translatedTypeParams.constraints

    override val type: SirType by lazyWithSessions {
        ktSymbol.expandedType.translateType(
            SirTypeVariance.INVARIANT,
            reportErrorType = { error("Can't translate ${ktSymbol.render()} type: $it") },
            reportUnsupportedType = { error("Can't translate ${ktSymbol.render()} type: it is not supported") },
            processTypeImports = ktSymbol.containingModule.sirModule()::updateImports,
            typeParameterScope = lexicalTypeParameterScope(),
        )
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            addAll(translatedAttributes)
            addDocumentationVisibility(kdocElements)
        }
    }
}
