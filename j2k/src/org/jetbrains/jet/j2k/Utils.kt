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

import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.jet.j2k.ast.*
import com.intellij.psi.util.PsiMethodUtil

fun quoteKeywords(packageName: String): String = packageName.split("\\.").map { Identifier.toKotlin(it) }.joinToString(".")

fun findVariableUsages(variable: PsiVariable, scope: PsiElement): Collection<PsiReferenceExpression> {
    return ReferencesSearch.search(variable, LocalSearchScope(scope)).findAll().filterIsInstance(javaClass<PsiReferenceExpression>())
}

fun findMethodCalls(method: PsiMethod, scope: PsiElement): Collection<PsiMethodCallExpression> {
    return ReferencesSearch.search(method, LocalSearchScope(scope)).findAll().map {
        if (it is PsiReferenceExpression) {
            val methodCall = it.getParent() as? PsiMethodCallExpression
            if (methodCall?.getMethodExpression() == it) methodCall else null
        }
        else {
            null
        }
    }.filterNotNull()
}

fun PsiVariable.countWriteAccesses(scope: PsiElement?): Int
        = if (scope != null) findVariableUsages(this, scope).count { PsiUtil.isAccessedForWriting(it) } else 0

fun PsiVariable.hasWriteAccesses(scope: PsiElement?): Boolean
        = if (scope != null) findVariableUsages(this, scope).any { PsiUtil.isAccessedForWriting(it) } else false

fun getDefaultInitializer(field: Field): Expression? {
    val t = field.`type`
    val result = if (t.isNullable) {
        LiteralExpression("null")
    }
    else if (t is PrimitiveType) {
        when (t.name.name) {
            "Boolean" -> LiteralExpression("false")
            "Char" -> LiteralExpression("' '")
            "Double" -> MethodCallExpression.buildNotNull(LiteralExpression("0").assignNoPrototype(), OperatorConventions.DOUBLE.toString())
            "Float" -> MethodCallExpression.buildNotNull(LiteralExpression("0").assignNoPrototype(), OperatorConventions.FLOAT.toString())
            else -> LiteralExpression("0")
        }
    }
    else {
        null
    }
    return result?.assignNoPrototype()
}

fun isVal(field: PsiField): Boolean {
    if (field.hasModifierProperty(PsiModifier.FINAL)) return true
    if (!field.hasModifierProperty(PsiModifier.PRIVATE)) return false
    val containingClass = field.getContainingClass() ?: return false
    val writes = findVariableUsages(field, containingClass).filter { PsiUtil.isAccessedForWriting(it) }
    if (writes.size == 0) return true
    if (writes.size > 1) return false
    val write = writes.single()
    val parent = write.getParent()
    if (parent is PsiAssignmentExpression &&
            parent.getOperationSign().getTokenType() == JavaTokenType.EQ &&
            isQualifierEmptyOrThis(write)) {
        val constructor = write.getContainingConstructor()
        return constructor != null &&
                constructor.getContainingClass() == containingClass &&
                parent.getParent() is PsiExpressionStatement &&
                parent.getParent()?.getParent() == constructor.getBody()
    }
    return false
}

fun shouldGenerateDefaultInitializer(field: PsiField)
        = field.getInitializer() == null && !(isVal(field) && field.hasWriteAccesses(field.getContainingClass()))

fun isQualifierEmptyOrThis(ref: PsiReferenceExpression): Boolean {
    val qualifier = ref.getQualifierExpression()
    return qualifier == null || (qualifier is PsiThisExpression && qualifier.getQualifier() == null)
}

fun PsiElement.isInSingleLine(): Boolean {
    if (this is PsiWhiteSpace) {
        val text = getText()!!
        return text.indexOf('\n') < 0 && text.indexOf('\r') < 0
    }

    var child = getFirstChild()
    while (child != null) {
        if (!child!!.isInSingleLine()) return false
        child = child!!.getNextSibling()
    }
    return true
}

fun PsiElement.getContainingMethod(): PsiMethod? {
    var context = getContext()
    while (context != null) {
        val _context = context!!
        if (_context is PsiMethod) return _context
        context = _context.getContext()
    }
    return null
}

fun PsiElement.getContainingConstructor(): PsiMethod? {
    val method = getContainingMethod()
    return if (method?.isConstructor() == true) method else null
}

fun PsiElement.isConstructor(): Boolean = this is PsiMethod && this.isConstructor()

fun PsiModifierListOwner.accessModifier(): String = when {
    hasModifierProperty(PsiModifier.PUBLIC) -> PsiModifier.PUBLIC
    hasModifierProperty(PsiModifier.PRIVATE) -> PsiModifier.PRIVATE
    hasModifierProperty(PsiModifier.PROTECTED) -> PsiModifier.PROTECTED
    else -> PsiModifier.PACKAGE_LOCAL
}

fun PsiMethod.isMainMethod(): Boolean = PsiMethodUtil.isMainMethod(this)

fun <T: Any> List<T>.singleOrNull2(): T? = if (size == 1) this[0] else null
fun <T: Any> Array<T>.singleOrNull2(): T? = if (size == 1) this[0] else null
