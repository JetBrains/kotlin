/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassConstructor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirContainingClassDetails
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirClassConstructorFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache

class ClassConstructorCommonizer(cache: CirClassifiersCache) : AbstractStandardCommonizer<CirClassConstructor, CirClassConstructor>() {
    private var isPrimary = false
    private lateinit var kind: CallableMemberDescriptor.Kind
    private val visibility = VisibilityCommonizer.equalizing()
    private val typeParameters = TypeParameterListCommonizer(cache)
    private val valueParameters = ValueParameterListCommonizer(cache)
    private var hasStableParameterNames = true
    private var hasSynthesizedParameterNames = false

    override fun commonizationResult() = CirClassConstructorFactory.create(
        annotations = emptyList(),
        typeParameters = typeParameters.result,
        visibility = visibility.result,
        containingClassDetails = CirContainingClassDetails.DOES_NOT_MATTER,
        valueParameters = valueParameters.result,
        hasStableParameterNames = hasStableParameterNames,
        hasSynthesizedParameterNames = hasSynthesizedParameterNames,
        isPrimary = isPrimary,
        kind = kind
    )

    override fun initialize(first: CirClassConstructor) {
        isPrimary = first.isPrimary
        kind = first.kind
    }

    override fun doCommonizeWith(next: CirClassConstructor): Boolean {
        val result = !next.containingClassDetails.kind.isSingleton // don't commonize constructors for objects and enum entries
                && next.containingClassDetails.modality != Modality.SEALED // don't commonize constructors for sealed classes (not not their subclasses)
                && isPrimary == next.isPrimary
                && kind == next.kind
                && visibility.commonizeWith(next)
                && typeParameters.commonizeWith(next.typeParameters)
                && valueParameters.commonizeWith(next.valueParameters)

        if (result) {
            hasStableParameterNames = hasStableParameterNames && next.hasStableParameterNames
            hasSynthesizedParameterNames = hasSynthesizedParameterNames || next.hasSynthesizedParameterNames
        }

        return result
    }
}
