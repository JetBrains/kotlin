/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/kir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.kir

/**
 * This interface describes elements (module or declaration) that have a name, define a scope and can appear as part of a Swift FQ name
 *
 * Generated from: [org.jetbrains.kotlin.kir.tree.generator.KirTree.scopeDefiningElement]
 */
sealed interface KirScopeDefiningElement : KirElement {
    val name: String
}
