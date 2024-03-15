/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.sir.impl

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

internal class SirInitImpl(
    override val origin: SirOrigin,
    override val visibility: SirVisibility,
    override var documentation: String?,
    override val kind: SirCallableKind,
    override var body: SirFunctionBody?,
    override val isFailable: Boolean,
    override val parameters: MutableList<SirParameter>,
    override val initKind: SirInitializerKind,
) : SirInit() {
    override lateinit var parent: SirDeclarationParent

    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {
    }
}
