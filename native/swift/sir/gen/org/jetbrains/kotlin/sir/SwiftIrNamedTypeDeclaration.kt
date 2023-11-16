/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.visitors.SwiftIrTransformer
import org.jetbrains.kotlin.sir.visitors.SwiftIrVisitor

/**
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.namedTypeDeclaration]
 */
sealed interface SwiftIrNamedTypeDeclaration : SwiftIrDeclarationWithName {
    override val origin: Origin
    override val attributes: List<Attribute>
    override val visibility: SwiftVisibility
    override var parent: SwiftIrDeclarationParent
    override val name: String

    override fun <R, D> accept(visitor: SwiftIrVisitor<R, D>, data: D): R =
        visitor.visitNamedTypeDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : SwiftIrElement, D> transform(transformer: SwiftIrTransformer<D>, data: D): E =
        transformer.transformNamedTypeDeclaration(this, data) as E
}
