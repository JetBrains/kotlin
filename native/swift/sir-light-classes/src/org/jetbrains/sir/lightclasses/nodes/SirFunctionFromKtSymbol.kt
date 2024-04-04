/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.sirCallableKind

internal class SirFunctionFromKtSymbol(
    override val ktSymbol: KtFunctionLikeSymbol,
    override val analysisApiSession: KtAnalysisSession,
    override val sirSession: SirSession,
) : SirFunction(), SirFromKtSymbol {

    override val origin: SirOrigin
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val kind: SirCallableKind
    override val name: String
    override val parameters: MutableList<SirParameter> = mutableListOf()
    override val returnType: SirType
    override var documentation: String?

    override var parent: SirDeclarationParent
        get() = withSirAnalyse(sirSession, analysisApiSession) {
            ktSymbol.getSirParent()
        }
        set(value) {
            // do nothing.
        }

    override var body: SirFunctionBody? = null

    init {
        withSirAnalyse(sirSession, analysisApiSession) {
            origin = KotlinSource(ktSymbol)

            kind = ktSymbol.sirCallableKind

            name = ktSymbol.sirDeclarationName()

            ktSymbol.valueParameters.mapTo(parameters) {
                SirParameter(
                    argumentName = it.name.asString(),
                    type = it.returnType.translateType()
                )
            }
            returnType = ktSymbol.returnType.translateType()
            documentation = ktSymbol.documentation()
        }

    }
}
