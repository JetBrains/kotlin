/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirTreeAccessor

public class SirTreeAccessorImpl : SirTreeAccessor {
    override fun SirModule.allImports(): Sequence<SirImport> =
        declarations.filterIsInstance<SirImport>().asSequence()

    override fun SirDeclarationContainer.allCallables(): Sequence<SirCallable> =
        declarations.filterIsInstance<SirCallable>().asSequence()

    override fun SirDeclarationContainer.allVariables(): Sequence<SirVariable> =
        declarations.filterIsInstance<SirVariable>().asSequence()

    override fun SirDeclarationContainer.allClasses(): Sequence<SirClass> =
        declarations.filterIsInstance<SirClass>().asSequence()

    override fun SirDeclarationContainer.allPackageEnums(): Sequence<SirEnum> = declarations
        .filterIsInstance<SirEnum>()
        .filter { it.origin is SirOrigin.Namespace }
        .asSequence()

    override fun SirDeclarationContainer.allNonPackageEnums(): Sequence<SirEnum> = declarations
        .filterIsInstance<SirEnum>()
        .filter { it.origin !is SirOrigin.Namespace }
        .asSequence()

    override fun SirModule.allExtensions(): Sequence<SirExtension> =
        declarations.filterIsInstance<SirExtension>().asSequence()
}