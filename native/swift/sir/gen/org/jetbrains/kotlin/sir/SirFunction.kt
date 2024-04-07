/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.function]
 */
abstract class SirFunction : SirCallable() {
    abstract override val origin: SirOrigin
    abstract override val visibility: SirVisibility
    abstract override var parent: SirDeclarationParent
    abstract override val kind: SirCallableKind
    abstract override var body: SirFunctionBody?
    abstract val name: String
    abstract val parameters: List<SirParameter>
    abstract val returnType: SirType
    abstract override var documentation: String?

    override fun <R, D> accept(visitor: SirVisitor<R, D>, data: D): R =
        visitor.visitFunction(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : SirElement, D> transform(transformer: SirTransformer<D>, data: D): E =
        transformer.transformFunction(this, data) as E
}
