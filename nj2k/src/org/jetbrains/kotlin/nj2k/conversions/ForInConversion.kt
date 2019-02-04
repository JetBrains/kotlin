/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.*
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId


class ForInConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKForInStatement) return recurse(element)

        val parameterDeclaration = element.declaration as? JKVariable
        if (parameterDeclaration != null
            && !context.converter.settings.specifyLocalVariableTypeByDefault
        ) {
            parameterDeclaration.type = JKTypeElementImpl(JKNoTypeImpl)
        }

        return recurse(element)
    }
}