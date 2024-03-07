/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.kt

import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.builder.buildClass
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

public object KotlinRuntimeModule : SirModule() {
    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {

    }

    override val declarations: List<SirDeclaration> by lazy {
        listOf(
            kotlinBase
        )
    }

    public val kotlinBase: SirClass = buildClass {
        name = "KotlinBase"
    }.also {
        it.parent = this
    }

    override val name: String
        get() = "KotlinRuntime"
}