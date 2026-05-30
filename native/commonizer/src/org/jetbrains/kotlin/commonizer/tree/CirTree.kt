/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers

data class CirTreeRoot(
    val modules: List<CirTreeModule> = [],
    val dependencies: CirProvidedClassifiers = CirProvidedClassifiers.EMPTY
)

data class CirTreeModule(
    val module: CirModule,
    val packages: List<CirTreePackage> = []
)

data class CirTreePackage(
    val pkg: CirPackage,
    val properties: List<CirProperty> = [],
    val functions: List<CirFunction> = [],
    val classes: List<CirTreeClass> = [],
    val typeAliases: List<CirTreeTypeAlias> = []
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
    val properties: List<CirProperty> = [],
    val functions: List<CirFunction> = [],
    val constructors: List<CirClassConstructor> = [],
    val classes: List<CirTreeClass> = [],
) : CirTreeClassifier


