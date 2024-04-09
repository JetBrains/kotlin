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
import org.jetbrains.sir.lightclasses.extensions.sirCallableKind

internal class SirVariableFromKtSymbol(
    override val ktSymbol: KtVariableSymbol,
    override val analysisApiSession: KtAnalysisSession,
    override val sirSession: SirSession,
) : SirVariable(), SirFromKtSymbol {

    override val origin: SirOrigin
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val name: String
    override val type: SirType
    override val getter: SirGetter
    override val setter: SirSetter?
    override var documentation: String?
    override var parent: SirDeclarationParent
        get() = withSirAnalyse(sirSession, analysisApiSession) {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    init {
        withSirAnalyse(sirSession, analysisApiSession) {
            origin = KotlinSource(ktSymbol)

            name = ktSymbol.sirDeclarationName()
            type = ktSymbol.returnType.translateType()

            val accessorKind = ktSymbol.sirCallableKind
            getter = buildGetter {
                kind = accessorKind
            }
            setter = if (!ktSymbol.isVal) buildSetter {
                kind = accessorKind
            } else null

            documentation = ktSymbol.documentation()
        }
    }

    init {
        getter.parent = this
        setter?.parent = this
    }
}
