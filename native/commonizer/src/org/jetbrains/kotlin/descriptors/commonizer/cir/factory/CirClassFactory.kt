/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirClassImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.filteredSupertypes
import org.jetbrains.kotlin.resolve.isInlineClass

object CirClassFactory {
    fun create(source: ClassDescriptor): CirClass = create(
        annotations = source.annotations.compactMap(CirAnnotationFactory::create),
        name = CirName.create(source.name),
        typeParameters = source.declaredTypeParameters.compactMap(CirTypeParameterFactory::create),
        visibility = source.visibility,
        modality = source.modality,
        kind = source.kind,
        companion = source.companionObjectDescriptor?.name?.let(CirName::create),
        isCompanion = source.isCompanionObject,
        isData = source.isData,
        isInline = source.isInlineClass(),
        isInner = source.isInner,
        isExternal = source.isExternal
    ).apply {
        setSupertypes(source.filteredSupertypes.compactMap { CirTypeFactory.create(it) })
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        name: CirName,
        typeParameters: List<CirTypeParameter>,
        visibility: DescriptorVisibility,
        modality: Modality,
        kind: ClassKind,
        companion: CirName?,
        isCompanion: Boolean,
        isData: Boolean,
        isInline: Boolean,
        isInner: Boolean,
        isExternal: Boolean
    ): CirClass {
        return CirClassImpl(
            annotations = annotations,
            name = name,
            typeParameters = typeParameters,
            visibility = visibility,
            modality = modality,
            kind = kind,
            companion = companion,
            isCompanion = isCompanion,
            isData = isData,
            isInline = isInline,
            isInner = isInner,
            isExternal = isExternal
        )
    }
}
