package com.intellij.codeInsight.highlighting

import com.intellij.psi.PsiReference

/**
 * Marker interface for references that should be highlighted with
 * [com.intellij.openapi.editor.DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE] text attributes.
 */
interface HighlightedReference : PsiReference