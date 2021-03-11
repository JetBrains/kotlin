/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.psi


import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.ide.konan.NativeDefinitionsLanguage

class NativeDefinitionsElementType(debugName: String) : IElementType(debugName, NativeDefinitionsLanguage.INSTANCE)

class NativeDefinitionsTokenType(debugName: String) : IElementType(debugName, NativeDefinitionsLanguage.INSTANCE) {
    override fun toString(): String = "NativeDefinitionsTokenType." + super.toString()
}

private class CodeEscaper(host: NativeDefinitionsCodeImpl) : LiteralTextEscaper<NativeDefinitionsCodeImpl>(host) {
    override fun isOneLine(): Boolean = false

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int = offsetInDecoded

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        outChars.append(myHost.text)
        return true
    }
}

class NativeDefinitionsCodeImpl(node: ASTNode) : ASTWrapperPsiElement(node), NativeDefinitionsCode, PsiLanguageInjectionHost {

    override fun updateText(text: String): PsiLanguageInjectionHost {
        return ElementManipulators.handleContentChange(this, text)
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return CodeEscaper(this)
    }

    fun accept(visitor: NativeDefinitionsVisitor) {
        visitor.visitCode(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is NativeDefinitionsVisitor) accept(visitor) else super.accept(
            visitor
        )
    }

    override fun isValidHost(): Boolean = true
}
