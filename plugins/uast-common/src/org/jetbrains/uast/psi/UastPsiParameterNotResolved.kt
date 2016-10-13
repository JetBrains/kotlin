package org.jetbrains.uast.psi

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.light.LightParameter
import org.jetbrains.uast.UastErrorType

class UastPsiParameterNotResolved(
        declarationScope: PsiElement,
        language: Language
) : LightParameter("error", UastErrorType, declarationScope, language)