/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.util.*

/**
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.module]
 */
abstract class SirModule : SirElementBase(), SirMutableDeclarationContainer, SirScopeDefiningElement {
    abstract override val declarations: MutableList<SirDeclaration>
    abstract override val name: String
    abstract val imports: MutableList<SirImport>
    override fun toString(): String {
        return this.debugString
    }
}
