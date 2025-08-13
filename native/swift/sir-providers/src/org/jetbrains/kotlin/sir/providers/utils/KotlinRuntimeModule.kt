/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.*
import org.jetbrains.kotlin.sir.providers.source.KotlinRuntimeElement
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule.kotlinBaseConstructionOptions
import org.jetbrains.kotlin.sir.util.SirSwiftModule

/**
 * Models `KotlinRuntime` module which contains declarations that are
 * required for integration with Kotlin/Native runtime.
 */
public object KotlinRuntimeModule : SirModule() {

    override val imports: MutableList<SirImport> = mutableListOf()

    override val name: String = "KotlinRuntime"

    override val declarations: MutableList<SirDeclaration> by lazy {
        mutableListOf(
            kotlinBaseConstructionOptions,
            kotlinBase
        )
    }

    public val kotlinBaseConstructionOptions: SirStruct = buildStruct { // Faux struct representing NS_ENUM(NSUInteger)
        origin = KotlinRuntimeElement()
        name = "KotlinBaseConstructionOptions"
    }.initializeParentForSelfAndChildren(KotlinRuntimeModule)

    public val kotlinBaseDesignatedInit: SirInit = buildKotlinBaseDesignatedInit()

    public val kotlinBase: SirClass by lazy {
        buildClass {
            name = "KotlinBase"
            origin = KotlinRuntimeElement()

            declarations += kotlinBaseDesignatedInit

            declarations += buildVariable {
                origin = KotlinRuntimeElement()
                name = "hash"
                type = SirNominalType(SirSwiftModule.int)
                getter = buildGetter {
                    origin = KotlinRuntimeElement()
                }
            }.also { it.getter.parent = it }
        }.initializeParentForSelfAndChildren(KotlinRuntimeModule)
    }
}

public object KotlinRuntimeSupportModule : SirModule() {
    override val imports: MutableList<SirImport> = mutableListOf()
    override val name: String = "KotlinRuntimeSupport"

    override val declarations: MutableList<SirDeclaration> by lazy {
        mutableListOf(
            kotlinError,
            kotlinBridgeable
        )
    }

    public val kotlinError: SirStruct = buildStruct {
        origin = KotlinRuntimeElement()
        name = "KotlinError"
        visibility = SirVisibility.PUBLIC
    }

    public val kotlinBridgeableInit: SirInit = buildKotlinBaseDesignatedInit()

    public val kotlinBridgeableExternalRcRef: SirFunction = buildFunction {
        origin = KotlinRuntimeElement()
        name = "__externalRCRef"
        returnType = SirNominalType(SirSwiftModule.unsafeMutableRawPointer).optional()
    }

    public val kotlinBridgeable: SirProtocol by lazy {
        buildProtocol {
            name = "_KotlinBridgeable"
            origin = KotlinRuntimeElement()

            declarations += kotlinBridgeableInit
            declarations += kotlinBridgeableExternalRcRef
        }.initializeParentForSelfAndChildren(KotlinRuntimeSupportModule)
    }

    public val kotlinBridgeableType: SirExistentialType = SirExistentialType(kotlinBridgeable)

    public val kotlinExistential: SirClass = buildClass {
        origin = KotlinRuntimeElement()
        name = "_KotlinExistential"
        visibility = SirVisibility.PUBLIC
        superClass = SirNominalType(KotlinRuntimeModule.kotlinBase)
        protocols.add(kotlinBridgeable)
    }.initializeParentForSelfAndChildren(KotlinRuntimeSupportModule)
}

private fun <T> T.initializeParentForSelfAndChildren(parentModule: SirModule): T where T : SirDeclaration, T : SirDeclarationContainer {
    parent = parentModule
    declarations.forEach { it.parent = this }
    return this
}

private fun buildKotlinBaseDesignatedInit(): SirInit = buildInit {
    origin = KotlinRuntimeElement()
    isFailable = false
    isOverride = false
    parameters.addAll(
        listOf(
            SirParameter(
                argumentName = "__externalRCRefUnsafe",
                type = SirNominalType(SirSwiftModule.unsafeMutableRawPointer).optional()
            ),
            SirParameter(
                argumentName = "options",
                type = SirNominalType(kotlinBaseConstructionOptions)
            ),
        )
    )
}
