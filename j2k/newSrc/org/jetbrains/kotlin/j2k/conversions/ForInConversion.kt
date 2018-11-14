/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId


class ForInConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKForInStatement) return recurse(element)

        val parameterDeclaration = element.declaration as? JKParameter
        if (parameterDeclaration != null) {
            parameterDeclaration.type = JKTypeElementImpl(JKNoTypeImpl)
        }

        return recurse(element)
    }
}