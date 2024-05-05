/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

object RhizomedbFirErrors {
    val MANY_ATTRIBUTE_NOT_A_SET by error0<PsiElement>()
    val NOT_ENTITY by error0<PsiElement>()

    init {
        RootDiagnosticRendererFactory.registerFactory(RhizomedbDefaultErrorMessages)
    }
}
