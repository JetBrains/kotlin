/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.ide

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.parcelize.ParcelizeCodegenExtension

class IdeParcelizeCodegenExtension : ParcelizeCodegenExtension() {
    override fun isAvailable(element: PsiElement): Boolean {
        return ParcelizeAvailability.isAvailable(element)
    }
}