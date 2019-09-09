/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassConstructor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonClassConstructor

class ClassConstructorCommonizer(cache: ClassifiersCache) : AbstractStandardCommonizer<ClassConstructor, ClassConstructor>() {
    private var isPrimary = false
    private lateinit var kind: CallableMemberDescriptor.Kind
    private val visibility = VisibilityCommonizer.equalizing()
    private val typeParameters = TypeParameterListCommonizer.default(cache)
    private val valueParameters = ValueParameterListCommonizer.default(cache)
    private var hasStableParameterNames = true
    private var hasSynthesizedParameterNames = false

    override fun commonizationResult() = CommonClassConstructor(
        isPrimary = isPrimary,
        kind = kind,
        visibility = visibility.result,
        typeParameters = typeParameters.result,
        valueParameters = valueParameters.result,
        hasStableParameterNames = hasStableParameterNames,
        hasSynthesizedParameterNames = hasSynthesizedParameterNames
    )

    override fun initialize(first: ClassConstructor) {
        isPrimary = first.isPrimary
        kind = first.kind
    }

    override fun doCommonizeWith(next: ClassConstructor): Boolean {
        val result = isPrimary == next.isPrimary
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
