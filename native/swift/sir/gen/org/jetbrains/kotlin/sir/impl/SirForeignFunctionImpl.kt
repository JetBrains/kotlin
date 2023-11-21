/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir.impl

import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirForeignFunction
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

internal class SirForeignFunctionImpl(
    override val origin: SirOrigin,
    override val visibility: SirVisibility,
) : SirForeignFunction() {
    override lateinit var parent: SirDeclarationParent

    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {
    }
}
