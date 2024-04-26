/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

public fun SirDeclarationContainer.allCallables(): Sequence<SirCallable> =
    declarations.asSequence().filterIsInstance<SirCallable>()

public fun SirDeclarationContainer.allVariables(): Sequence<SirVariable> =
    declarations.asSequence().filterIsInstance<SirVariable>()

public fun SirDeclarationContainer.allClasses(): Sequence<SirClass> =
    declarations.asSequence().filterIsInstance<SirClass>()

public fun SirDeclarationContainer.allTypealiases(): Sequence<SirTypealias> =
    declarations.asSequence().filterIsInstance<SirTypealias>()

public fun SirDeclarationContainer.allContainers(): Sequence<SirDeclarationContainer> =
    declarations.asSequence().filterIsInstance<SirDeclarationContainer>()

public fun SirDeclarationContainer.allPackageEnums(): Sequence<SirEnum> = declarations
    .asSequence()
    .filterIsInstance<SirEnum>()
    .filter { it.origin is SirOrigin.Namespace }

public fun SirDeclarationContainer.allNonPackageEnums(): Sequence<SirEnum> = declarations
    .asSequence()
    .filterIsInstance<SirEnum>()
    .filter { it.origin !is SirOrigin.Namespace }

public fun SirModule.allExtensions(): Sequence<SirExtension> =
    declarations.asSequence().filterIsInstance<SirExtension>()