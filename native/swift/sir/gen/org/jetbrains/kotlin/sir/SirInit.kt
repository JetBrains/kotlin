/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

/**
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.init]
 */
abstract class SirInit : SirElementBase(), SirCallable {
    abstract override val origin: SirOrigin
    abstract override val visibility: SirVisibility
    abstract override val documentation: String?
    abstract override var parent: SirDeclarationParent
    abstract override val attributes: List<SirAttribute>
    abstract override var body: SirFunctionBody?
    abstract override val errorType: SirType
    abstract val isFailable: Boolean
    abstract val parameters: List<SirParameter>
    abstract val isConvenience: Boolean
    abstract val isRequired: Boolean
    abstract val isOverride: Boolean
}
