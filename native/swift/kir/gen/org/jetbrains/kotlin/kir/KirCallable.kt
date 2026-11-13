/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/kir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.kir

/**
 * Generated from: [org.jetbrains.kotlin.kir.tree.generator.KirTree.callable]
 */
sealed interface KirCallable : KirDeclaration, KirBridged {
    override val origin: KirOrigin
    override val visibility: KirVisibility
    override val documentation: String?
    override var parent: KirDeclarationParent
    override val bridges: List<KirBridge>
    var body: KirFunctionBody?
}
