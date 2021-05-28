/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirDeclaration
import org.jetbrains.kotlin.storage.NullableLazyValue


internal fun interface CommonizerCondition {

    fun allowCommonization(): Boolean

    infix fun and(other: CommonizerCondition) = CommonizerCondition { allowCommonization() && other.allowCommonization() }

    companion object Factory {
        fun none(): CommonizerCondition = NoneCommonizerCondition

        fun nodeIsNotCommonized(node: CirNode<*, *>) = CommonizerCondition { node.commonDeclaration() == null }

        fun parent(parent: CirNode<*, *>): ParentNodeCommonizerCondition = ParentNodeCommonizerCondition(parent.commonDeclaration)

        fun parent(parent: CirNode<*, *>?): CommonizerCondition = if (parent == null) none() else parent(parent)
    }
}

private object NoneCommonizerCondition : CommonizerCondition {
    override fun allowCommonization(): Boolean = true
}

internal class ParentNodeCommonizerCondition(
    private val parentNodeCommonDeclaration: NullableLazyValue<CirDeclaration>
) : CommonizerCondition {
    override fun allowCommonization(): Boolean = parentNodeCommonDeclaration() != null
}
