/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kir.lightclasses.nodes

import org.jetbrains.kir.lightclasses.KirFromSirDeclaration
import org.jetbrains.kir.lightclasses.extensions.lazyWithSession
import org.jetbrains.kir.lightclasses.extensions.withSession
import org.jetbrains.kotlin.kir.KirBridge
import org.jetbrains.kotlin.kir.KirDeclarationParent
import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirFunctionBody
import org.jetbrains.kotlin.kir.KirOrigin
import org.jetbrains.kotlin.kir.KirVisibility
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.getKirParent
import org.jetbrains.kotlin.kir.providers.kirDeclarationName
import org.jetbrains.kotlin.kir.providers.source.SirSource
import org.jetbrains.kotlin.kir.providers.utils.toKirVisibility
import org.jetbrains.kotlin.sir.SirFunction

internal class KirFunctionFromSirFunction(
    override val sirDeclaration: SirFunction,
    override val kirSession: KirSession,
) : KirFunction(), KirFromSirDeclaration<SirFunction> {

    override val origin: KirOrigin by lazy {
        SirSource(sirDeclaration)
    }
    override val visibility: KirVisibility by lazy {
        sirDeclaration.visibility.toKirVisibility()
    }
    override val name: String by lazyWithSession {
        sirDeclaration.kirDeclarationName()
    }
    override val documentation: String? get() = sirDeclaration.documentation

    override var parent: KirDeclarationParent
        get() = withSession {
            sirDeclaration.getKirParent()
        }
        set(_) = Unit

    override val bridges: List<KirBridge> get() = emptyList()

    override var body: KirFunctionBody? = null
}
