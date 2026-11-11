/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kir.lightclasses.nodes

import org.jetbrains.kir.lightclasses.KirFromSirDeclaration
import org.jetbrains.kir.lightclasses.extensions.withSession
import org.jetbrains.kir.lightclasses.utils.reverseBridgeName
import org.jetbrains.kir.lightclasses.utils.toKirBridge
import org.jetbrains.kotlin.kir.KirBridge
import org.jetbrains.kotlin.kir.KirConstructor
import org.jetbrains.kotlin.kir.KirDeclarationParent
import org.jetbrains.kotlin.kir.KirFunctionBody
import org.jetbrains.kotlin.kir.KirOrigin
import org.jetbrains.kotlin.kir.KirVisibility
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.getKirParent
import org.jetbrains.kotlin.kir.providers.source.SirSource
import org.jetbrains.kotlin.kir.providers.utils.toKirVisibility
import org.jetbrains.kotlin.sir.SirInit

internal class KirConstructorFromSirInit(
    override val sirDeclaration: SirInit,
    override val kirSession: KirSession,
) : KirConstructor(), KirFromSirDeclaration<SirInit> {

    override val origin: KirOrigin by lazy {
        SirSource(sirDeclaration)
    }
    override val visibility: KirVisibility by lazy {
        sirDeclaration.visibility.toKirVisibility()
    }
    override val documentation: String? get() = sirDeclaration.documentation

    override var parent: KirDeclarationParent
        get() = withSession {
            sirDeclaration.getKirParent()
        }
        set(_) = Unit

    override val bridges: List<KirBridge> by lazy {
        sirDeclaration.bridges.map { it.toKirBridge() }
    }

    /**
     * The argument of the delegation to the primary constructor: the Swift object that is created by the bridge.
     * Parameters are not supported yet.
     */
    override val delegationCallArguments: KirFunctionBody = KirFunctionBody(
        listOf(
            sirDeclaration.reverseBridgeName
                ?.let { bridgeName -> "kotlinx.cinterop.interpretObjCPointer<platform.darwin.NSObject>($bridgeName())" }
                ?: "TODO(\"Bridge is absent\")"
        )
    )

    override var body: KirFunctionBody? = sirDeclaration.reverseBridgeName?.let { KirFunctionBody(listOf()) }
}
