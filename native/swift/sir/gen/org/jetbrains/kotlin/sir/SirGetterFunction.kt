/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

/**
 * An interface that marks getter functions for a property
 *
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.getterFunction]
 */
interface SirGetterFunction : SirAccessorFunction {
    override val origin: SirOrigin
    override val visibility: SirVisibility
    override val documentation: String?
    override var parent: SirDeclarationParent
    override val attributes: List<SirAttribute>
    override val variableName: String
    override val getter: SirFunction
    override val setter: SirFunction?
}
