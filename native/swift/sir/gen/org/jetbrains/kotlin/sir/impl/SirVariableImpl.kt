/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.sir.impl

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

internal class SirVariableImpl(
    override val origin: SirOrigin,
    override val visibility: SirVisibility,
    override val name: String,
    override val type: SirType,
    override val getter: SirGetter,
    override val setter: SirSetter?,
    override val isStatic: Boolean,
) : SirVariable() {
    override lateinit var parent: SirDeclarationParent

    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {
        getter.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {
    }
}
