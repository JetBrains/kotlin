/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

/**
 * This interface describes declarations that have a name, define a scope and can appear as part of a Swift FQ name
 *
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.scopeDefiningDeclaration]
 */
sealed interface SirScopeDefiningDeclaration : SirDeclaration, SirScopeDefiningElement {
    override val origin: SirOrigin
    override val visibility: SirVisibility
    override val documentation: String?
    override var parent: SirDeclarationParent
    override val attributes: List<SirAttribute>
    override val name: String
}
