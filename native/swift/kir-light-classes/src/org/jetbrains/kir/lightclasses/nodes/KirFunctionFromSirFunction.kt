/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kir.lightclasses.nodes

import org.jetbrains.kir.lightclasses.KirFromSirDeclaration
import org.jetbrains.kir.lightclasses.extensions.lazyWithSession
import org.jetbrains.kir.lightclasses.extensions.withSession
import org.jetbrains.kotlin.kir.CFunctionBridge
import org.jetbrains.kotlin.kir.KirBridge
import org.jetbrains.kotlin.kir.KirDeclarationParent
import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirFunctionBridge
import org.jetbrains.kotlin.kir.KirFunctionBody
import org.jetbrains.kotlin.kir.KirOrigin
import org.jetbrains.kotlin.kir.KirVisibility
import org.jetbrains.kotlin.kir.KotlinFunctionBridge
import org.jetbrains.kotlin.kir.SwiftFunctionBridge
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.getKirParent
import org.jetbrains.kotlin.kir.providers.kirDeclarationName
import org.jetbrains.kotlin.kir.providers.source.SirSource
import org.jetbrains.kotlin.kir.providers.utils.toKirVisibility
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBridge
import org.jetbrains.kotlin.sir.SirReverseFunctionBridge
import org.jetbrains.kotlin.sir.SirTypeBindingBridge

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

    override val bridges: List<KirBridge> by lazy {
        sirDeclaration.bridges.map { it.toKirBridge() }
    }

    /**
     * Calls the bridge that is bound to the Swift function.
     * Parameters and return values are not supported yet, so the call is just the invocation of the bridge.
     */
    override var body: KirFunctionBody? = sirDeclaration.bridges
        .filterIsInstance<SirReverseFunctionBridge>()
        .singleOrNull()
        ?.let { KirFunctionBody(listOf("${it.name}()")) }
}

private fun SirBridge.toKirBridge(): KirBridge = when (this) {
    is SirReverseFunctionBridge -> KirFunctionBridge(
        name = name,
        kotlinFunctionBridge = KotlinFunctionBridge(
            lines = kotlinFunctionBridge.lines,
            packageDependencies = kotlinFunctionBridge.packageDependencies,
        ),
        swiftFunctionBridge = SwiftFunctionBridge(lines = swiftFunctionBridge.lines),
        cDeclarationBridge = CFunctionBridge(
            lines = cDeclarationBridge.lines,
            headerDependencies = cDeclarationBridge.headerDependencies,
        ),
    )
    is SirFunctionBridge, is SirTypeBindingBridge -> error("Bridge '$name' is not supported in Swift import yet")
}
