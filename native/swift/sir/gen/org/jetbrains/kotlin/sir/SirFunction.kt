/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

/**
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.function]
 */
abstract class SirFunction : SirElementBase(), SirCallable, SirClassMemberDeclaration {
    abstract override val origin: SirOrigin
    abstract override val visibility: SirVisibility
    abstract override val documentation: String?
    abstract override var parent: SirDeclarationParent
    abstract override val attributes: List<SirAttribute>
    abstract override var body: SirFunctionBody?
    abstract override val errorType: SirType
    abstract override val isOverride: Boolean
    abstract override val isInstance: Boolean
    abstract override val modality: SirModality
    abstract val name: String
    abstract val extensionReceiverParameter: SirParameter?
    abstract val parameters: List<SirParameter>
    abstract val returnType: SirType
}
