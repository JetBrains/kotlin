/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirModule

class SirPlatformModule(override val name: String) : SirModule() {
    override val declarations: MutableList<SirDeclaration> = mutableListOf()
    override val imports: MutableList<SirImport> = mutableListOf()
}