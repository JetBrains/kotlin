/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k

import org.jetbrains.jet.j2k.ast.Identifier
import com.intellij.psi.PsiElement
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNewExpression
import org.jetbrains.jet.j2k.ast.Field
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.PsiModifierListOwner

fun quoteKeywords(packageName: String): String = packageName.split("\\.").map { Identifier(it).toKotlin() }.makeString(".")

fun PsiElement.countWriteAccesses(scope: PsiElement?): Int {
    if (scope == null) return 0

    var writes = 0
    scope.accept(object: JavaRecursiveElementVisitor() {
        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            super.visitReferenceExpression(expression)
            if (PsiUtil.isAccessedForWriting(expression) && expression.isReferenceTo(this@countWriteAccesses)) {
                writes++
            }
        }
    })
    return writes
}

fun PsiModifierListOwner.isAnnotatedAsNotNull(): Boolean
        = getModifierList()?.getAnnotations()?.any { NOT_NULL_ANNOTATIONS.contains(it.getQualifiedName()) } ?: false

fun PsiElement?.isDefinitelyNotNull(): Boolean = when(this) {
    is PsiLiteralExpression -> getValue() != null
    is PsiNewExpression -> true
    else -> false
}

fun getDefaultInitializer(field: Field): String {
    if (field.`type`.isNullable) {
        return "null"
    }
    else {
        return when(field.`type`.toKotlin()) {
            "Boolean" -> "false"
            "Char" -> "' '"
            "Double" -> "0." + OperatorConventions.DOUBLE + "()"
            "Float" -> "0." + OperatorConventions.FLOAT + "()"
            else -> "0"
        }
    }
}