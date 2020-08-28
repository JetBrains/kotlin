/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.name.ClassId

sealed class CirClassifierId {
    interface ClassOrTypeAlias {
        val classId: ClassId
    }

    data class Class(override val classId: ClassId) : ClassOrTypeAlias, CirClassifierId()
    data class TypeAlias(override val classId: ClassId) : ClassOrTypeAlias, CirClassifierId()
    data class TypeParameter(val index: Int) : CirClassifierId()
}
