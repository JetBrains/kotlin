/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.util.*

/**
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.enum]
 */
abstract class SirEnum : SirElementBase(), SirNamedDeclaration, SirMutableDeclarationContainer, SirProtocolConformingDeclaration {
    abstract override val origin: SirOrigin
    abstract override val visibility: SirVisibility
    abstract override val documentation: String?
    abstract override var parent: SirDeclarationParent
    abstract override val attributes: List<SirAttribute>
    abstract override val name: String
    abstract override val declarations: MutableList<SirDeclaration>
    abstract override val protocols: List<SirProtocol>
    abstract val cases: List<SirEnumCase>
    override fun toString(): String {
        return this.debugString
    }
}
