/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class CirClassImpl(
    override val annotations: List<CirAnnotation>,
    override val name: Name,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: Visibility,
    override val modality: Modality,
    override val kind: ClassKind,
    override var companion: FqName?,
    override val isCompanion: Boolean,
    override val isData: Boolean,
    override val isInline: Boolean,
    override val isInner: Boolean,
    override val isExternal: Boolean,
    override val supertypes: MutableCollection<CirType>
) : CirClass
