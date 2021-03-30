/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility

interface CirClass : CirClassifier, CirContainingClass {
    var companion: CirName? // null means no companion object
    val isCompanion: Boolean
    val isValue: Boolean
    val isInner: Boolean
    val isExternal: Boolean
    var supertypes: List<CirType>

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline fun create(
            annotations: List<CirAnnotation>,
            name: CirName,
            typeParameters: List<CirTypeParameter>,
            visibility: Visibility,
            modality: Modality,
            kind: ClassKind,
            companion: CirName?,
            isCompanion: Boolean,
            isData: Boolean,
            isValue: Boolean,
            isInner: Boolean,
            isExternal: Boolean
        ): CirClass = CirClassImpl(
            annotations = annotations,
            name = name,
            typeParameters = typeParameters,
            visibility = visibility,
            modality = modality,
            kind = kind,
            companion = companion,
            isCompanion = isCompanion,
            isData = isData,
            isValue = isValue,
            isInner = isInner,
            isExternal = isExternal
        )
    }
}

data class CirClassImpl(
    override val annotations: List<CirAnnotation>,
    override val name: CirName,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: Visibility,
    override val modality: Modality,
    override val kind: ClassKind,
    override var companion: CirName?,
    override val isCompanion: Boolean,
    override val isData: Boolean,
    override val isValue: Boolean,
    override val isInner: Boolean,
    override val isExternal: Boolean,
) : CirClass {
    private var _supertypes: List<CirType>? = null

    override var supertypes: List<CirType>
        get() = _supertypes ?: error("${::supertypes.name} has not been initialized yet")
        set(value) {
            check(_supertypes == null) { "Re-initialization of ${::supertypes.name}" }
            _supertypes = value
        }
}
