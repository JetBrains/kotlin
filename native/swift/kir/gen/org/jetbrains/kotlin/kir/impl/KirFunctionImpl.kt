/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/kir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.kir.impl

import org.jetbrains.kotlin.kir.*

internal class KirFunctionImpl(
    override val origin: KirOrigin,
    override val visibility: KirVisibility,
    override val documentation: String?,
    override val bridges: MutableList<KirBridge>,
    override var body: KirFunctionBody?,
    override val name: String,
) : KirFunction() {
    override lateinit var parent: KirDeclarationParent
}
