/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.commonizer.utils.ANY_CLASS_ID

object CirStandardTypes {
    val ANY: CirClassType = CirClassType.createInterned(
        classId = ANY_CLASS_ID,
        outerType = null,
        visibility = Visibilities.Public,
        arguments = emptyList(),
        isMarkedNullable = false
    )
}
