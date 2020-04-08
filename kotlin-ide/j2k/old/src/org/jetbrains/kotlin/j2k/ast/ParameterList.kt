/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.j2k.ast

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.append

class ParameterList(
        val parameters: List<Parameter>,
        val lPar: LPar?,
        val rPar: RPar?
) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        lPar?.let { builder.append(it) }

        builder.append(parameters, ", ")

        rPar?.let { builder.append(it) }
    }

    companion object {
        fun withNoPrototype(parameters: List<Parameter>): ParameterList {
            return ParameterList(parameters, LPar.withPrototype(null), RPar.withPrototype(null)).assignNoPrototype()
        }
    }
}

// we use LPar and RPar elements to better handle comments and line breaks around them
class LPar private constructor() : Element() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("(")
    }

    companion object {
        fun withPrototype(element: PsiElement?) = LPar().assignPrototype(element, CommentsAndSpacesInheritance.LINE_BREAKS)
    }
}

class RPar private constructor() : Element() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(")")
    }

    companion object {
        fun withPrototype(element: PsiElement?) = RPar().assignPrototype(element, CommentsAndSpacesInheritance.LINE_BREAKS)
    }
}
