/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree.mergers

import kotlinx.metadata.KmFunction
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirTargetMergingContext
import org.jetbrains.kotlin.commonizer.mergedtree.buildFunctionNode
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers
import org.jetbrains.kotlin.commonizer.utils.isFakeOverride
import org.jetbrains.kotlin.commonizer.utils.isKniBridgeFunction
import org.jetbrains.kotlin.commonizer.utils.isTopLevelDeprecatedFunction

internal object FunctionMerger {
    fun processFunction(
        context: CirTargetMergingContext,
        ownerNode: CirNodeWithMembers<*, *>,
        function: KmFunction
    ) = with(context) {
        if (function.isFakeOverride()
            || function.isKniBridgeFunction()
            || function.isTopLevelDeprecatedFunction(isTopLevel = ownerNode !is CirClassNode)
        ) {
            return
        }

        val maybeClassOwnerNode: CirClassNode? = ownerNode as? CirClassNode

        val approximationKey = FunctionApproximationKey(function, context.typeResolver)
        val functionNode: CirFunctionNode = ownerNode.functions.getOrPut(approximationKey) {
            buildFunctionNode(storageManager, targets, classifiers, maybeClassOwnerNode?.commonDeclaration)
        }
        functionNode.targetDeclarations[context.targetIndex] = CirDeserializers.function(
            name = approximationKey.name,
            source = function,
            containingClass = maybeClassOwnerNode?.targetDeclarations?.get(context.targetIndex),
            typeResolver = context.typeResolver
        )
    }
}
