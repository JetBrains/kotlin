/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildClass
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.providers.source.KotlinRuntimeElement

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
                kind = SirCallableKind.CLASS_METHOD
                isFailable = false
                initKind = SirInitializerKind.ORDINARY
                isOverride = false
            }
        }.also { klass ->
            klass.parent = KotlinRuntimeModule
            klass.declarations.forEach { it.parent = klass }
        }
    }
}