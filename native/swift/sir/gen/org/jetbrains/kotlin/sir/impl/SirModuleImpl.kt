/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.sir.impl

import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirModule

internal class SirModuleImpl(
    override val declarations: MutableList<SirDeclaration>,
    override val name: String,
    override val imports: MutableList<SirImport>,
) : SirModule() {
}
