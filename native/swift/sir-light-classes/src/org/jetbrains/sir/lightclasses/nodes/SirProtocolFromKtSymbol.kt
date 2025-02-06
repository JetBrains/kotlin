/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.containingModule
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.translatedAttributes

internal class SirProtocolFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val ktModule: KaModule,
    override val sirSession: SirSession,
) : SirProtocol(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val origin: SirOrigin = KotlinSource(ktSymbol)
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override val name: String by lazy {
        ktSymbol.name.asString()
    }
    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(useSiteSession)
        }
        set(_) = Unit

    override val superClass: SirType? by lazy {
        SirNominalType(KotlinRuntimeModule.kotlinBase)
    }

    override val protocols: List<SirProtocol> by lazyWithSessions {
        ktSymbol.superTypes
            .mapNotNull { it.symbol as? KaClassSymbol }
            .filter { it.classKind == KaClassKind.INTERFACE }
            .mapNotNull {
                it.toSir().allDeclarations.firstIsInstanceOrNull<SirProtocol>()?.also {
                    ktSymbol.containingModule.sirModule().updateImport(SirImport(it.containingModule().name))
                }
            }
    }

    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }

    override val declarations: List<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations(useSiteSession)
            .toList()
    }
}