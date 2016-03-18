/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast.java

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UType
import org.jetbrains.uast.UastContext

class JavaUType(
        val psi: PsiType?,
        override val parent: UElement
) : JavaAbstractUElement(), UType {
    override val name: String
        get() = when (psi) {
            is PsiClassType -> psi.className.substringAfterLast('.')
            else -> psi?.canonicalText?.substringAfterLast('.')
        }.orAnonymous("type")

    override val fqName: String?
        get() = when (psi) {
            is PsiClassType -> psi.resolve()?.qualifiedName
            else -> null
        }

    override val isInt: Boolean
        get() = check("int", "java.lang.Integer")

    override val isLong: Boolean
        get() = check("long", "java.lang.Long")

    override val isFloat: Boolean
        get() = check("float", "java.lang.Float")

    override val isDouble: Boolean
        get() = check("double", "java.lang.Double")

    override val isChar: Boolean
        get() = check("char", "java.lang.Character")

    override val isBoolean: Boolean
        get() = check("boolean", "java.lang.Boolean")

    override val isByte: Boolean
        get() = check("byte", "java.lang.Byte")

    private inline fun check(unboxedType: String, boxedType: String): Boolean =
            name == unboxedType || (psi as? PsiClassType)?.resolve()?.qualifiedName == boxedType

    override val annotations by lz { psi.getAnnotations(this) }

    override fun resolve(context: UastContext) = when (psi) {
        is PsiClassType -> psi.resolve()?.let { context.convert(it) as? UClass }
        else -> null
    }
}