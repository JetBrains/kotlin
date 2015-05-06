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

package org.jetbrains.kotlin.j2k

import org.jetbrains.kotlin.types.expressions.OperatorConventions
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.j2k.ast.*
import com.intellij.psi.util.PsiMethodUtil

fun quoteKeywords(packageName: String): String = packageName.split('.').map { Identifier.toKotlin(it) }.joinToString(".")

fun PsiVariable.countWriteAccesses(searcher: ReferenceSearcher, scope: PsiElement?): Int
        = if (scope != null) searcher.findVariableUsages(this, scope).count { PsiUtil.isAccessedForWriting(it) } else 0

fun PsiVariable.hasWriteAccesses(searcher: ReferenceSearcher, scope: PsiElement?): Boolean
        = if (scope != null) searcher.findVariableUsages(this, scope).any { PsiUtil.isAccessedForWriting(it) } else false

fun getDefaultInitializer(property: Property): Expression? {
    val t = property.type
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

fun isVal(searcher: ReferenceSearcher, field: PsiField): Boolean {
    if (field.hasModifierProperty(PsiModifier.FINAL)) return true
    if (!field.hasModifierProperty(PsiModifier.PRIVATE)) return false
    val containingClass = field.getContainingClass() ?: return false
    val writes = searcher.findVariableUsages(field, containingClass).filter { PsiUtil.isAccessedForWriting(it) }
    if (writes.size() == 0) return true
    if (writes.size() > 1) return false
    val write = writes.single()
    val parent = write.getParent()
    if (parent is PsiAssignmentExpression &&
            parent.getOperationSign().getTokenType() == JavaTokenType.EQ &&
            write.isQualifierEmptyOrThis()) {
        val constructor = write.getContainingConstructor()
        return constructor != null &&
                constructor.getContainingClass() == containingClass &&
                parent.getParent() is PsiExpressionStatement &&
                parent.getParent()?.getParent() == constructor.getBody()
    }
    return false
}

fun shouldGenerateDefaultInitializer(searcher: ReferenceSearcher, field: PsiField)
        = field.getInitializer() == null && !(isVal(searcher, field) && field.hasWriteAccesses(searcher, field.getContainingClass()))

fun PsiReferenceExpression.isQualifierEmptyOrThis(): Boolean {
    val qualifier = getQualifierExpression()
    return qualifier == null || (qualifier is PsiThisExpression && qualifier.getQualifier() == null)
}

fun PsiReferenceExpression.isQualifierEmptyOrClass(psiClass: PsiClass): Boolean {
    val qualifier = getQualifierExpression()
    return qualifier == null || (qualifier is PsiReferenceExpression && qualifier.isReferenceTo(psiClass))
}

fun PsiElement.isInSingleLine(): Boolean {
    if (this is PsiWhiteSpace) {
        val text = getText()!!
        return text.indexOf('\n') < 0 && text.indexOf('\r') < 0
    }

    var child = getFirstChild()
    while (child != null) {
        if (!child.isInSingleLine()) return false
        child = child.getNextSibling()
    }
    return true
}

//TODO: check for variables that are definitely assigned in constructors
fun PsiElement.getContainingMethod(): PsiMethod? {
    var context = getContext()
    while (context != null) {
        val _context = context
        if (_context is PsiMethod) return _context
        context = _context.getContext()
    }
    return null
}

fun PsiElement.getContainingConstructor(): PsiMethod? {
    val method = getContainingMethod()
    return if (method?.isConstructor() == true) method else null
}

fun PsiMember.isConstructor(): Boolean = this is PsiMethod && this.isConstructor()

fun PsiModifierListOwner.accessModifier(): String = when {
    hasModifierProperty(PsiModifier.PUBLIC) -> PsiModifier.PUBLIC
    hasModifierProperty(PsiModifier.PRIVATE) -> PsiModifier.PRIVATE
    hasModifierProperty(PsiModifier.PROTECTED) -> PsiModifier.PROTECTED
    else -> PsiModifier.PACKAGE_LOCAL
}

fun PsiMethod.isMainMethod(): Boolean = PsiMethodUtil.isMainMethod(this)

fun PsiMember.isImported(file: PsiJavaFile): Boolean {
    if (this is PsiClass) {
        val fqName = getQualifiedName()
        val index = fqName?.lastIndexOf('.') ?: -1
        val parentName = if (index >= 0) fqName!!.substring(0, index) else null
        return file.getImportList()?.getAllImportStatements()?.any {
            it.getImportReference()?.getQualifiedName() == (if (it.isOnDemand()) parentName else fqName)
        } ?: false
    }
    else {
        return getContainingClass() != null && file.getImportList()?.getImportStaticStatements()?.any {
            it.resolveTargetClass() == getContainingClass() && (it.isOnDemand() || it.getReferenceName() == getName())
        } ?: false
    }
}

