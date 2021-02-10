/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmClass
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirClassImpl
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeClassKind
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeModality
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeVisibility
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

    fun create(name: CirName, source: KmClass, typeResolver: CirTypeResolver): CirClass = create(
        annotations = CirAnnotationFactory.createAnnotations(source.flags, typeResolver, source::annotations),
        name = name,
        typeParameters = source.typeParameters.compactMap { CirTypeParameterFactory.create(it, typeResolver) },
        visibility = decodeVisibility(source.flags),
        modality = decodeModality(source.flags),
        kind = decodeClassKind(source.flags),
        companion = source.companionObject?.let(CirName::create),
        isCompanion = Flag.Class.IS_COMPANION_OBJECT(source.flags),
        isData = Flag.Class.IS_DATA(source.flags),
        isInline = Flag.Class.IS_INLINE(source.flags),
        isInner = Flag.Class.IS_INNER(source.flags),
        isExternal = Flag.Class.IS_EXTERNAL(source.flags)
    ).apply {
        setSupertypes(source.filteredSupertypes.compactMap { CirTypeFactory.create(it, typeResolver) })
    }

    fun createDefaultEnumEntry(
        name: CirName,
        annotations: List<KmAnnotation>,
        enumClassId: CirEntityId,
        enumClass: KmClass,
        typeResolver: CirTypeResolver
    ): CirClass = create(
        annotations = annotations.compactMap { CirAnnotationFactory.create(it, typeResolver) },
        name = name,
        typeParameters = emptyList(),
        visibility = DescriptorVisibilities.PUBLIC,
        modality = Modality.FINAL,
        kind = ClassKind.ENUM_ENTRY,
        companion = null,
        isCompanion = false,
        isData = false,
        isInline = false,
        isInner = false,
        isExternal = false
    ).apply {
        val enumClassType = CirTypeFactory.createClassType(
            classId = enumClassId,
            outerType = null,
            visibility = decodeVisibility(enumClass.flags),
            arguments = emptyList(),
            isMarkedNullable = false
        )
        setSupertypes(listOf(enumClassType))
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
