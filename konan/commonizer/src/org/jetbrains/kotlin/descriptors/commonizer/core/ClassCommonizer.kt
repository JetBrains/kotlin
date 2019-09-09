/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassDeclaration
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonClassDeclaration
import org.jetbrains.kotlin.name.Name

class ClassCommonizer(cache: ClassifiersCache) : AbstractStandardCommonizer<ClassDeclaration, ClassDeclaration>() {
    private lateinit var name: Name
    private lateinit var kind: ClassKind
    private val typeParameters = TypeParameterListCommonizer.default(cache)
    private val modality = ModalityCommonizer.default()
    private val visibility = VisibilityCommonizer.equalizing()
    private var isInner = false
    private var isInline = false
    private var isCompanion = false

    override fun commonizationResult() = CommonClassDeclaration(
        name = name,
        typeParameters = typeParameters.result,
        kind = kind,
        modality = modality.result,
        visibility = visibility.result,
        isCompanion = isCompanion,
        isInline = isInline,
        isInner = isInner
    )

    override fun initialize(first: ClassDeclaration) {
        name = first.name
        kind = first.kind
        isInner = first.isInner
        isInline = first.isInline
        isCompanion = first.isCompanion
    }

    override fun doCommonizeWith(next: ClassDeclaration) =
        kind == next.kind
                && isInner == next.isInner
                && isInline == next.isInline
                && isCompanion == next.isCompanion
                && next.sealedSubclasses.isEmpty() // commonization of sealed classes is not supported, but their subclasses can be commonized
                && modality.commonizeWith(next.modality)
                && visibility.commonizeWith(next)
                && typeParameters.commonizeWith(next.typeParameters)
}
