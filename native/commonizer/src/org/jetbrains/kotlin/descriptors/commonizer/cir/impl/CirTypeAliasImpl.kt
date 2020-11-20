/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.name.Name

data class CirTypeAliasImpl(
    override val annotations: List<CirAnnotation>,
    override val name: Name,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: DescriptorVisibility,
    override val underlyingType: CirClassOrTypeAliasType,
    override val expandedType: CirClassType // only for commonization algorithm; does not participate in building resulting declarations
) : CirTypeAlias {
    // any TA in "common" fragment is already lifted up
    override val isLiftedUp get() = true
}
