/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.sir.impl

import org.jetbrains.kotlin.sir.*

internal class SirClassImpl(
    override val origin: SirOrigin,
    override val visibility: SirVisibility,
    override val documentation: String?,
    override val name: String,
    override val declarations: MutableList<SirDeclaration>,
    override val superClass: SirType?,
) : SirClass() {
    override lateinit var parent: SirDeclarationParent
}
