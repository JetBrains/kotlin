/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildClass
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildProtocol
import org.jetbrains.kotlin.sir.builder.buildStruct
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
            kotlinBase
        )
    }

    public val kotlinBase: SirClass by lazy {
        buildClass {
            name = "KotlinBase"
            origin = KotlinRuntimeElement()
            declarations += buildInit {
                origin = KotlinRuntimeElement()
                isFailable = false
                isOverride = false
            }
            declarations += buildInit {
                origin = KotlinRuntimeElement()
                isFailable = false
                isOverride = false
                parameters.add(
                    SirParameter(
                        argumentName = "__externalRCRef",
                        type = SirNominalType(SirSwiftModule.uint)
                    )
                )
            }
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
}