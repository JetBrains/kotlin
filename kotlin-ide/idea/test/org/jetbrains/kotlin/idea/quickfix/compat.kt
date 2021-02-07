/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl

// BUNCH: 193
fun invokeIntentionCompat(action: IntentionAction, file: PsiFile, editor: Editor) {
    CodeInsightTestFixtureImpl.invokeIntention(action, file, editor)
}