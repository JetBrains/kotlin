/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.utils.ANY_CLASS_ID

object CirStandardTypes {
    val ANY: CirClassType = CirClassType.createInterned(
        classId = ANY_CLASS_ID,
        outerType = null,
        arguments = emptyList(),
        isMarkedNullable = false
    )
}
