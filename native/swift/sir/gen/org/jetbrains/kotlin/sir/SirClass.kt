/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.util.*

/**
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.class]
 */
abstract class SirClass : SirBridged(), SirScopeDefiningDeclaration, SirDeclarationContainer, SirClassInhertingDeclaration, SirProtocolConformingDeclaration {
    abstract override val origin: SirOrigin
    abstract override val visibility: SirVisibility
    abstract override val documentation: String?
    abstract override var parent: SirDeclarationParent
    abstract override val attributes: List<SirAttribute>
    abstract override val name: String
    abstract override val declarations: List<SirDeclaration>
    abstract override val superClass: SirNominalType?
    abstract override val protocols: List<SirProtocol>
    abstract override val bridges: List<SirBridge>
    abstract val modality: SirModality
    override fun toString(): String {
        return this.debugString
    }
}
