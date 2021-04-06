/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree.mergers

import kotlinx.metadata.KmConstructor
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirTargetMergingContext
import org.jetbrains.kotlin.commonizer.mergedtree.buildClassConstructorNode
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers

internal object ClassConstructorMerger {
    fun processClassConstructor(
        context: CirTargetMergingContext,
        classNode: CirClassNode,
        constructor: KmConstructor
    ) = with(context) {
        val approximationKey = ConstructorApproximationKey(constructor, context.typeResolver)
        val constructorNode: CirClassConstructorNode = classNode.constructors.getOrPut(approximationKey) {
            buildClassConstructorNode(storageManager, targets, classifiers, classNode.commonDeclaration)
        }
        constructorNode.targetDeclarations[context.targetIndex] = CirDeserializers.constructor(
            source = constructor,
            containingClass = classNode.targetDeclarations[context.targetIndex]!!,
            typeResolver = context.typeResolver
        )
    }
}
