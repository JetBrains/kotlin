/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree.mergers

import kotlinx.metadata.KmProperty
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirTargetMergingContext
import org.jetbrains.kotlin.commonizer.mergedtree.buildPropertyNode
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers
import org.jetbrains.kotlin.commonizer.utils.isFakeOverride

internal object PropertyMerger {
    fun processProperty(
        context: CirTargetMergingContext,
        ownerNode: CirNodeWithMembers<*, *>,
        property: KmProperty
    ) = with(context) {
        if (property.isFakeOverride())
            return

        val maybeClassOwnerNode: CirClassNode? = ownerNode as? CirClassNode

        val approximationKey = PropertyApproximationKey(property, context.typeResolver)
        val propertyNode: CirPropertyNode = ownerNode.properties.getOrPut(approximationKey) {
            buildPropertyNode(storageManager, targets, classifiers, maybeClassOwnerNode?.commonDeclaration)
        }
        propertyNode.targetDeclarations[context.targetIndex] = CirDeserializers.property(
            name = approximationKey.name,
            source = property,
            containingClass = maybeClassOwnerNode?.targetDeclarations?.get(context.targetIndex),
            typeResolver = context.typeResolver
        )
    }
}
