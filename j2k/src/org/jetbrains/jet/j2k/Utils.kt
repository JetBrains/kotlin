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
import org.jetbrains.jet.j2k.ast.Field
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import java.util.ArrayList
import org.jetbrains.jet.j2k.ast.Nullability
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil

fun quoteKeywords(packageName: String): String = packageName.split("\\.").map { Identifier(it).toKotlin() }.makeString(".")

fun findVariableReferences(variable: PsiVariable, scope: PsiElement): Collection<PsiReferenceExpression> {
    class Visitor : JavaRecursiveElementVisitor() {
        val refs = ArrayList<PsiReferenceExpression>()

        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            super.visitReferenceExpression(expression)
            if (expression.isReferenceTo(variable)) {
                refs.add(expression)
            }
        }
    }

    val visitor = Visitor()
    scope.accept(visitor)
    return visitor.refs
}

fun PsiVariable.countWriteAccesses(scope: PsiElement?): Int
        = if (scope != null) findVariableReferences(this, scope).count { PsiUtil.isAccessedForWriting(it) } else 0

fun PsiModifierListOwner.nullabilityFromAnnotations(): Nullability {
    val annotations = getModifierList()?.getAnnotations() ?: return Nullability.Default
    return if (annotations.any { NOT_NULL_ANNOTATIONS.contains(it.getQualifiedName()) })
        Nullability.NotNull
    else if (annotations.any { NULLABLE_ANNOTATIONS.contains(it.getQualifiedName()) })
        Nullability.Nullable
    else
        Nullability.Default
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

fun isQualifierEmptyOrThis(ref: PsiReferenceExpression): Boolean {
    val qualifier = ref.getQualifierExpression()
    return qualifier == null || (qualifier is PsiThisExpression && qualifier.getQualifier() == null)
}
