/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kir.lightclasses.nodes

import org.jetbrains.kir.lightclasses.KirFromSirDeclaration
import org.jetbrains.kir.lightclasses.extensions.lazyWithSession
import org.jetbrains.kir.lightclasses.extensions.withSession
import org.jetbrains.kir.lightclasses.utils.reverseBridgeName
import org.jetbrains.kir.lightclasses.utils.toKirBridge
import org.jetbrains.kotlin.kir.KirBridge
import org.jetbrains.kotlin.kir.KirDeclarationParent
import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirFunctionBody
import org.jetbrains.kotlin.kir.KirModality
import org.jetbrains.kotlin.kir.KirOrigin
import org.jetbrains.kotlin.kir.KirVisibility
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.getKirParent
import org.jetbrains.kotlin.kir.providers.kirDeclarationName
import org.jetbrains.kotlin.kir.providers.source.SirSource
import org.jetbrains.kotlin.kir.providers.utils.toKirModality
import org.jetbrains.kotlin.kir.providers.utils.toKirVisibility
import org.jetbrains.kotlin.sir.SirClass
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

    override val isOverride: Boolean get() = sirDeclaration.isOverride

    override val isInstance: Boolean get() = sirDeclaration.isInstance

    override val modality: KirModality get() = sirDeclaration.modality.toKirModality()

    override val bridges: List<KirBridge> by lazy {
        sirDeclaration.bridges.map { it.toKirBridge() }
    }

    /**
     * Calls the bridge that is bound to the Swift function. A method passes the Swift object it is called on as `self`.
     * Other parameters and return values are not supported yet.
     */
    override var body: KirFunctionBody? = sirDeclaration.reverseBridgeName?.let { bridgeName ->
        val selfArgument = if (sirDeclaration.isInstance && sirDeclaration.parent is SirClass) "__ptr()" else ""
        KirFunctionBody(listOf("$bridgeName($selfArgument)"))
    }
}
