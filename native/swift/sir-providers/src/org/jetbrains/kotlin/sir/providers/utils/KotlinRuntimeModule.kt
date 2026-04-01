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
            kotlinBridgeable,
            kotlinExistential,
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

public object KotlinCoroutineSupportModule : SirModule() {
    override val imports: MutableList<SirImport> = mutableListOf()
    override val name: String = "KotlinCoroutineSupport"

    override val declarations: MutableList<SirDeclaration> by lazy {
        mutableListOf(
            swiftJob,
        )
    }

    public val swiftJob: SirClass = buildClass {
        origin = KotlinRuntimeElement()
        name = "KotlinTask"
        visibility = SirVisibility.PUBLIC
        modality = SirModality.FINAL
        superClass = KotlinRuntimeModule.kotlinBase.nominalType()
        protocols.add(KotlinRuntimeSupportModule.kotlinBridgeable)
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinTypedFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedFlowImpl: SirStruct = buildStruct {
        origin = KotlinRuntimeElement()
        name = "_KotlinTypedFlowImpl"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinSharedFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinSharedFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedSharedFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinTypedSharedFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedSharedFlowImpl: SirStruct = buildStruct {
        origin = KotlinRuntimeElement()
        name = "_KotlinTypedSharedFlowImpl"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinMutableSharedFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinMutableSharedFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedMutableSharedFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinTypedMutableSharedFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedMutableSharedFlowImpl: SirStruct = buildStruct {
        origin = KotlinRuntimeElement()
        name = "_KotlinTypedMutableSharedFlowImpl"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinStateFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinStateFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedStateFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinTypedStateFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedStateFlowImpl: SirStruct = buildStruct {
        origin = KotlinRuntimeElement()
        name = "_KotlinTypedStateFlowImpl"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinMutableStateFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinMutableStateFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedMutableStateFlow: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "KotlinTypedMutableStateFlow"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

    public val kotlinTypedMutableStateFlowImpl: SirStruct = buildStruct {
        origin = KotlinRuntimeElement()
        name = "_KotlinTypedMutableStateFlowImpl"
        visibility = SirVisibility.PUBLIC
    }.initializeParentForSelfAndChildren(KotlinCoroutineSupportModule)

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
