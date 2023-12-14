/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

internal fun KtSymbol.getKDocString(): String? {
    val psi = psi
    if (psi is KtDeclaration) {
        if (psi is KtPrimaryConstructor)
            return null  // to be rendered with class itself
        val kdoc = psi.docComment
        if (kdoc != null) {
            return kdoc.getDefaultSection().parent.text
        }
    }
    return null
}
