/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClassConstructor
import org.jetbrains.kotlin.commonizer.cir.CirContainingClass
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality

class ClassConstructorCommonizer(
    classifiers: CirKnownClassifiers
) : AbstractStandardCommonizer<CirClassConstructor, CirClassConstructor>() {
    private var isPrimary = false
    private val visibility = VisibilityCommonizer.equalizing()
    private val typeParameters = TypeParameterListCommonizer(classifiers)
    private val valueParameters = CallableValueParametersCommonizer(classifiers)
    private val annotationsCommonizer = AnnotationsCommonizer()

    override fun commonizationResult(): CirClassConstructor {
        val valueParameters = valueParameters.result
        valueParameters.patchCallables()

        return CirClassConstructor.create(
            annotations = annotationsCommonizer.result,
            typeParameters = typeParameters.result,
            visibility = visibility.result,
            containingClass = CONTAINING_CLASS_DOES_NOT_MATTER, // does not matter
            valueParameters = valueParameters.valueParameters,
            hasStableParameterNames = valueParameters.hasStableParameterNames,
            isPrimary = isPrimary
        )
    }

    override fun initialize(first: CirClassConstructor) {
        isPrimary = first.isPrimary
    }

    override fun doCommonizeWith(next: CirClassConstructor): Boolean {
        return !next.containingClass.kind.isSingleton // don't commonize constructors for objects and enum entries
                && next.containingClass.modality != Modality.SEALED // don't commonize constructors for sealed classes (not not their subclasses)
                && isPrimary == next.isPrimary
                && visibility.commonizeWith(next)
                && typeParameters.commonizeWith(next.typeParameters)
                && valueParameters.commonizeWith(next)
                && annotationsCommonizer.commonizeWith(next.annotations)
    }

    companion object {
        private val CONTAINING_CLASS_DOES_NOT_MATTER = object : CirContainingClass {
            override val modality get() = Modality.FINAL
            override val kind get() = ClassKind.CLASS
            override val isData get() = false
        }
    }
}
