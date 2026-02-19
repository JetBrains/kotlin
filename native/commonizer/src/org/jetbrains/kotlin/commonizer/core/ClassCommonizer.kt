/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClass
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.ClassKind

class ClassCommonizer internal constructor(
    typeCommonizer: TypeCommonizer,
    supertypesCommonizer: ClassSuperTypeCommonizer
) : AbstractStandardCommonizer<CirClass, CirClass?>() {
    private lateinit var name: CirName
    private lateinit var kind: ClassKind
    private var isInner = false
    private var isValue = false
    private var isCompanion = false
    private var hasEnumEntries = false
    private val supertypesCommonizer = supertypesCommonizer.asCommonizer()
    private val typeParameterListCommonizer: TypeParameterListCommonizer = TypeParameterListCommonizer(typeCommonizer)
    private val modalityCommonizer: ModalityCommonizer = ModalityCommonizer()
    private val visibilityCommonizer: VisibilityCommonizer = VisibilityCommonizer.equalizing()
    private val annotationCommonizer = AnnotationsCommonizer.asCommonizer()

    override fun commonizationResult(): CirClass? {
        return CirClass.create(
            annotations = annotationCommonizer.result,
            name = name,
            typeParameters = typeParameterListCommonizer.result ?: return null,
            supertypes = supertypesCommonizer.result,
            visibility = visibilityCommonizer.result,
            modality = modalityCommonizer.result,
            kind = kind,
            companion = null,
            isCompanion = isCompanion,
            isData = false,
            isValue = isValue,
            isInner = isInner,
            hasEnumEntries = hasEnumEntries
        )
    }

    override fun initialize(first: CirClass) {
        name = first.name
        kind = first.kind
        isInner = first.isInner
        isValue = first.isValue
        isCompanion = first.isCompanion
        hasEnumEntries = first.hasEnumEntries
    }

    override fun doCommonizeWith(next: CirClass): Boolean {
        this.hasEnumEntries = this.hasEnumEntries && next.hasEnumEntries

        return kind == next.kind
                && isInner == next.isInner
                && isValue == next.isValue
                && isCompanion == next.isCompanion
                && modalityCommonizer.commonizeWith(next.modality)
                && visibilityCommonizer.commonizeWith(next)
                && typeParameterListCommonizer.commonizeWith(next.typeParameters)
                && supertypesCommonizer.commonizeWith(next.supertypes)
                && annotationCommonizer.commonizeWith(next.annotations)
    }
}
