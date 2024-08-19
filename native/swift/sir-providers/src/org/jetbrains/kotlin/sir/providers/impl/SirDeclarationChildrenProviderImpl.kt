/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirChildrenProvider
import org.jetbrains.kotlin.sir.providers.SirSession

public class SirDeclarationChildrenProviderImpl(private val sirSession: SirSession) : SirChildrenProvider {

    override fun KaScope.extractDeclarations(ktAnalysisSession: KaSession): Sequence<SirDeclaration> =
        declarations
            .filter {
                with(sirSession) { it.sirVisibility(ktAnalysisSession) == SirVisibility.PUBLIC }
            }.flatMap {
                sequence {
                    with(sirSession) {
                        val declaration = it.sirDeclaration()
                        yield(declaration)
                        yieldAll(declaration.trampolineDeclarations())
                        if (declaration is SirClass) {
                            declaration.generateAdapterDeclarations()
                        }
                    }
                }
            }
}
