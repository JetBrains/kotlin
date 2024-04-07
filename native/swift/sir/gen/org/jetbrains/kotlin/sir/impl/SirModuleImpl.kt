/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.sir.impl

import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.util.transformInPlace
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

internal class SirModuleImpl(
    override val declarations: MutableList<SirDeclaration>,
    override val name: String,
) : SirModule() {

    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {
        declarations.transformInPlace(transformer, data)
    }
}
