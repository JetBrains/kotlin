/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.PsiElement

// TODO [beresnev] get rid of
internal fun PsiElement.resolveToGetDri(): PsiElement? =
    reference?.resolve()
