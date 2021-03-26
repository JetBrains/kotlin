/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.ConstructorApproximationKey
import org.jetbrains.kotlin.commonizer.mergedtree.FunctionApproximationKey
import org.jetbrains.kotlin.commonizer.mergedtree.PropertyApproximationKey

data class CirTreeRoot(
    val modules: List<CirTreeModule>
)

data class CirTreeModule(
    val module: CirModule,
    val packages: List<CirTreePackage>
)

data class CirTreePackage(
    val pkg: CirPackage,
    val properties: List<CirTreeProperty>,
    val functions: List<CirTreeFunction>,
    val classes: List<CirTreeClass>,
    val typeAliases: List<CirTreeTypeAlias>
)

data class CirTreeProperty(
    val approximationKey: PropertyApproximationKey,
    val property: CirProperty
)

data class CirTreeFunction(
    val approximationKey: FunctionApproximationKey,
    val function: CirFunction
)

sealed interface CirTreeClassifier {
    val id: CirEntityId
}

data class CirTreeTypeAlias(
    override val id: CirEntityId,
    val typeAlias: CirTypeAlias
) : CirTreeClassifier

data class CirTreeClass(
    override val id: CirEntityId,
    val clazz: CirClass,
    val properties: List<CirTreeProperty>,
    val functions: List<CirTreeFunction>,
    val constructors: List<CirTreeClassConstructor>,
    val classes: List<CirTreeClass>,
) : CirTreeClassifier

class CirTreeClassConstructor(
    val approximationKey: ConstructorApproximationKey,
    val constructor: CirClassConstructor
)

