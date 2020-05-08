/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirClassConstructor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirCommonClassConstructor

class ClassConstructorCommonizer(cache: CirClassifiersCache) : AbstractStandardCommonizer<CirClassConstructor, CirClassConstructor>() {
    private var isPrimary = false
    private lateinit var kind: CallableMemberDescriptor.Kind
    private val visibility = VisibilityCommonizer.equalizing()
    private val typeParameters = TypeParameterListCommonizer(cache)
    private val valueParameters = ValueParameterListCommonizer(cache)
    private var hasStableParameterNames = true
    private var hasSynthesizedParameterNames = false

    override fun commonizationResult() = CirCommonClassConstructor(
        isPrimary = isPrimary,
        kind = kind,
        visibility = visibility.result,
        typeParameters = typeParameters.result,
        valueParameters = valueParameters.result,
        hasStableParameterNames = hasStableParameterNames,
        hasSynthesizedParameterNames = hasSynthesizedParameterNames
    )

    override fun initialize(first: CirClassConstructor) {
        isPrimary = first.isPrimary
        kind = first.kind
    }

    override fun doCommonizeWith(next: CirClassConstructor): Boolean {
        val result = !next.containingClassKind.isSingleton // don't commonize constructors for objects and enum entries
                && next.containingClassModality != Modality.SEALED // don't commonize constructors for sealed classes (not not their subclasses)
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
