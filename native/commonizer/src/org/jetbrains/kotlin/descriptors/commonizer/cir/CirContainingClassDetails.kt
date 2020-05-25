/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality

// TODO: inline?
data class CirContainingClassDetails(
    val kind: ClassKind,
    val modality: Modality,
    val isData: Boolean
) {
    companion object {
        val DOES_NOT_MATTER = CirContainingClassDetails(
            kind = ClassKind.CLASS,
            modality = Modality.FINAL,
            isData = false
        )
    }
}
