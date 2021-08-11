/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClassOrTypeAliasType
import org.jetbrains.kotlin.commonizer.cir.CirType
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

class ClassSuperTypeCommonizer(
    private val classifiers: CirKnownClassifiers
) : AssociativeCommonizer<List<CirType>> {

    private val typeCommonizer = TypeCommonizer(classifiers)

    override fun commonize(first: List<CirType>, second: List<CirType>): List<CirType> {
        if (first.isEmpty() || second.isEmpty()) return emptyList()

        val firstGroup = first.filterIsInstance<CirClassOrTypeAliasType>().associateBy { it.classifierId }
        val secondGroup = second.filterIsInstance<CirClassOrTypeAliasType>().associateBy { it.classifierId }

        val commonClassifiers = firstGroup.keys intersect secondGroup.keys

        return commonClassifiers.mapNotNull { classifier ->
            typeCommonizer.commonize(firstGroup.getValue(classifier), secondGroup.getValue(classifier))
        }
    }

}