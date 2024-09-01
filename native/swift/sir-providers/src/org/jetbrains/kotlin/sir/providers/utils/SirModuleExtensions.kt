/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule

public fun SirModule.updateImport(newImport: SirImport) {
    updateImports(listOf(newImport))
}

public fun SirModule.updateImports(newImports: List<SirImport>) {
    imports += newImports
        .filter { it.moduleName != SirSwiftModule.name && it.moduleName != this.name }
        .filter { it !in imports }
}

public fun SirDeclaration.containingModule(): SirModule = when (val parent = parent) {
    is SirModule -> parent
    is SirDeclaration -> parent.containingModule()
}
