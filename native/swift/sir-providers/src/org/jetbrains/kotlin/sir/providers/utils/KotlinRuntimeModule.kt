/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.*
import org.jetbrains.kotlin.sir.providers.source.KotlinRuntimeElement
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
    }.also { struct ->
        struct.parent = KotlinRuntimeModule
        struct.declarations.forEach { it.parent = struct }
    }

    public val kotlinBaseDesignatedInit: SirInit = buildInit {
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
        }.also { klass ->
            klass.parent = KotlinRuntimeModule
            klass.declarations.forEach { it.parent = klass }
        }
    }
}

public object KotlinRuntimeSupportModule : SirModule() {
    override val imports: MutableList<SirImport> = mutableListOf()
    override val name: String = "KotlinRuntimeSupport"

    override val declarations: MutableList<SirDeclaration> by lazy {
        mutableListOf(
            kotlinError,
            kotlinBridged,
        )
    }

    public val kotlinError: SirStruct = buildStruct {
        origin = KotlinRuntimeElement()
        name = "KotlinError"
        visibility = SirVisibility.PUBLIC
    }

    public val kotlinBridged: SirProtocol = buildProtocol {
        origin = KotlinRuntimeElement()
        name = "_KotlinBridged"
        visibility = SirVisibility.PUBLIC
        superClass = SirNominalType(KotlinRuntimeModule.kotlinBase)
    }.also { proto ->
        proto.parent = KotlinRuntimeSupportModule
        proto.declarations.forEach { it.parent = proto }
    }

    public val kotlinExistential: SirClass = buildClass {
        origin = KotlinRuntimeElement()
        name = "_KotlinExistential"
        visibility = SirVisibility.PUBLIC
        superClass = SirNominalType(KotlinRuntimeModule.kotlinBase)
        protocols.add(kotlinBridged)
    }.also { declaration ->
        declaration.parent = KotlinRuntimeSupportModule
        declaration.declarations.forEach { it.parent = declaration }
    }
}
