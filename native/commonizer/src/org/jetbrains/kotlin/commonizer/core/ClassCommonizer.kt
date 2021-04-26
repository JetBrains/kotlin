/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClass
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.descriptors.ClassKind

class ClassCommonizer(classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirClass, CirClass>() {
    private lateinit var name: CirName
    private lateinit var kind: ClassKind
    private val typeParameters = TypeParameterListCommonizer(classifiers)
    private val modality = ModalityCommonizer()
    private val visibility = VisibilityCommonizer.equalizing()
    private var isInner = false
    private var isValue = false
    private var isCompanion = false

    override fun commonizationResult() = CirClass.create(
        annotations = emptyList(),
        name = name,
        typeParameters = typeParameters.result,
        visibility = visibility.result,
        modality = modality.result,
        kind = kind,
        companion = null,
        isCompanion = isCompanion,
        isData = false,
        isValue = isValue,
        isInner = isInner,
        isExternal = false
    )

    override fun initialize(first: CirClass) {
        name = first.name
        kind = first.kind
        isInner = first.isInner
        isValue = first.isValue
        isCompanion = first.isCompanion
    }

    override fun doCommonizeWith(next: CirClass) =
        kind == next.kind
                && isInner == next.isInner
                && isValue == next.isValue
                && isCompanion == next.isCompanion
                && modality.commonizeWith(next.modality)
                && visibility.commonizeWith(next)
                && typeParameters.commonizeWith(next.typeParameters)
}
