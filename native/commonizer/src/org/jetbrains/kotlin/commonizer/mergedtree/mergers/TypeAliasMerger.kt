/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree.mergers

import kotlinx.metadata.KmTypeAlias
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.mergedtree.CirPackageNode
import org.jetbrains.kotlin.commonizer.mergedtree.CirTargetMergingContext
import org.jetbrains.kotlin.commonizer.mergedtree.CirTypeAliasNode
import org.jetbrains.kotlin.commonizer.mergedtree.buildTypeAliasNode
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers

internal object TypeAliasMerger {
    fun processTypeAlias(
        context: CirTargetMergingContext,
        packageNode: CirPackageNode,
        typeAlias: KmTypeAlias
    ) = with(context) {
        val typeAliasName = CirName.create(typeAlias.name)
        val typeAliasId = CirEntityId.create(packageNode.packageName, typeAliasName)

        val typeAliasNode: CirTypeAliasNode = packageNode.typeAliases.getOrPut(typeAliasName) {
            buildTypeAliasNode(storageManager, targets, classifiers, typeAliasId)
        }
        typeAliasNode.targetDeclarations[context.targetIndex] = CirDeserializers.typeAlias(
            name = typeAliasName,
            source = typeAlias,
            typeResolver = context.typeResolver
        )
    }
}
