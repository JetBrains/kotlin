/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*

data class CirClassConstructorImpl(
    override val annotations: List<CirAnnotation>,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: Visibility,
    override val containingClassDetails: CirContainingClassDetails,
    override var valueParameters: List<CirValueParameter>,
    override var hasStableParameterNames: Boolean,
    override val isPrimary: Boolean,
    override val kind: CallableMemberDescriptor.Kind
) : CirClassConstructor
