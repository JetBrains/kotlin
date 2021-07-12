/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirPropertySetter
import org.jetbrains.kotlin.descriptors.Visibilities

object PropertySetterCommonizer : AssociativeCommonizer<CirPropertySetter?> {
    override fun commonize(first: CirPropertySetter?, second: CirPropertySetter?): CirPropertySetter? {
        if (first == null && second == null) return null
        if (first != null && second == null) return privateFallbackSetter
        if (first == null && second != null) return privateFallbackSetter

        if (first != null && second != null) {
            if (Visibilities.compare(first.visibility, second.visibility) == 0) {
                return CirPropertySetter.createDefaultNoAnnotations(first.visibility)
            }

            return privateFallbackSetter
        }

        return null
    }

    val privateFallbackSetter = CirPropertySetter.createDefaultNoAnnotations(Visibilities.Private)
}