/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.namedTypeDeclaration]
 */
sealed interface SirNamedTypeDeclaration : SirDeclarationWithName {
    override val origin: SirDeclarationOrigin
    override val visibility: SirVisibility
    override var parent: SirDeclarationParent
    override val name: String

    override fun <R, D> accept(visitor: SirVisitor<R, D>, data: D): R =
        visitor.visitNamedTypeDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : SirElement, D> transform(transformer: SirTransformer<D>, data: D): E =
        transformer.transformNamedTypeDeclaration(this, data) as E
}

val builtinsModule = buildModule {  }

sealed class BuiltinSirTypeDeclaration(
    override val name: String,
    swiftModule: SirModule,
) : SirNamedTypeDeclaration {
    override val origin: SirDeclarationOrigin = SirBuiltinDeclarationOrigin()
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override var parent: SirDeclarationParent = swiftModule

    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {

    }

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {

    }


    object Bool : BuiltinSirTypeDeclaration("Bool", builtinsModule)
    object Int8 : BuiltinSirTypeDeclaration("Int8", builtinsModule)
    object Int16 : BuiltinSirTypeDeclaration("Int16", builtinsModule)
    object Int32 : BuiltinSirTypeDeclaration("Int32", builtinsModule)
    object Int64 : BuiltinSirTypeDeclaration("Int64", builtinsModule)
}
