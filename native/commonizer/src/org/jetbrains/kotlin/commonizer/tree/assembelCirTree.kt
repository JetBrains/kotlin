/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree

import org.jetbrains.kotlin.commonizer.cir.CirClass
import org.jetbrains.kotlin.commonizer.cir.CirTypeAlias
import org.jetbrains.kotlin.commonizer.mergedtree.*

internal fun CirRootNode.assembleCirTree(): CirTreeRoot {
    return CirTreeRoot(
        modules = modules.values.mapNotNull { it.assembleCirTree() }
    )
}

internal fun CirModuleNode.assembleCirTree(): CirTreeModule? {
    return CirTreeModule(
        module = commonDeclaration() ?: return null,
        packages = packages.values.mapNotNull { it.assembleCirTree() }
    )
}

internal fun CirPackageNode.assembleCirTree(): CirTreePackage? {
    val commonizedPackage = commonDeclaration() ?: return null
    val commonizedTypeAliases = typeAliases.mapNotNull { (_, typeAlias) -> typeAlias.assembleCirTree() }

    return CirTreePackage(
        pkg = commonizedPackage,
        properties = properties.mapNotNull { (key, property) -> property.assembleCirTree(key) },
        functions = functions.mapNotNull { (key, function) -> function.assembleCirTree(key) },
        typeAliases = commonizedTypeAliases.filterIsInstance<CirTreeTypeAlias>(),
        classes = classes.mapNotNull { (_, clazz) -> clazz.assembleCirTree() } + commonizedTypeAliases.filterIsInstance<CirTreeClass>()
    )
}

internal fun CirClassNode.assembleCirTree(): CirTreeClass? {
    return CirTreeClass(
        id = id,
        clazz = commonDeclaration() ?: return null,
        properties = properties.mapNotNull { (key, property) -> property.assembleCirTree(key) },
        functions = functions.mapNotNull { (key, function) -> function.assembleCirTree(key) },
        constructors = constructors.mapNotNull { (key, constructor) -> constructor.assembleCirTree(key) },
        classes = classes.mapNotNull { (_, clazz) -> clazz.assembleCirTree() }
    )
}

internal fun CirTypeAliasNode.assembleCirTree(): CirTreeClassifier? {
    return when (val commonDeclaration = commonDeclaration()) {
        is CirTypeAlias -> CirTreeTypeAlias(id, commonDeclaration)
        is CirClass -> CirTreeClass(id, commonDeclaration)
        else -> null
    }
}

internal fun CirPropertyNode.assembleCirTree(approximationKey: PropertyApproximationKey): CirTreeProperty? {
    return CirTreeProperty(
        approximationKey = approximationKey,
        property = commonDeclaration() ?: return null,
    )
}

internal fun CirFunctionNode.assembleCirTree(approximationKey: FunctionApproximationKey): CirTreeFunction? {
    return CirTreeFunction(
        approximationKey = approximationKey,
        function = commonDeclaration() ?: return null,
    )
}

internal fun CirClassConstructorNode.assembleCirTree(approximationKey: ConstructorApproximationKey): CirTreeClassConstructor? {
    return CirTreeClassConstructor(
        approximationKey = approximationKey,
        constructor = commonDeclaration() ?: return null
    )
}

