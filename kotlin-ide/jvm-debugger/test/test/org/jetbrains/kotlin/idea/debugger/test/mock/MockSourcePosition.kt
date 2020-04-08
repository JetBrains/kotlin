/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.mock

import com.intellij.debugger.SourcePosition
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class MockSourcePosition(
    private val myFile: PsiFile? = null,
    private val myElementAt: PsiElement? = null,
    private val myLine: Int? = null,
    private val myOffset: Int? = null,
    private val myEditor: Editor? = null
) : SourcePosition() {
    override fun getFile(): PsiFile {
        return myFile ?: throw UnsupportedOperationException("Parameter file isn't set for MockSourcePosition")
    }

    override fun getElementAt(): PsiElement {
        return myElementAt ?: throw UnsupportedOperationException("Parameter elementAt isn't set for MockSourcePosition")
    }

    override fun getLine(): Int {
        return myLine ?: throw UnsupportedOperationException("Parameter line isn't set for MockSourcePosition")
    }

    override fun getOffset(): Int {
        return myOffset ?: throw UnsupportedOperationException("Parameter offset isn't set for MockSourcePosition")
    }

    override fun openEditor(requestFocus: Boolean): Editor {
        return myEditor ?: throw UnsupportedOperationException("Parameter editor isn't set for MockSourcePosition")
    }

    override fun navigate(requestFocus: Boolean) {
        throw UnsupportedOperationException("navigate() isn't supported for MockSourcePosition")
    }

    override fun canNavigate(): Boolean {
        throw UnsupportedOperationException("canNavigate() isn't supported for MockSourcePosition")
    }

    override fun canNavigateToSource(): Boolean {
        throw UnsupportedOperationException("canNavigateToSource() isn't supported for MockSourcePosition")
    }
}
