/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.psi.*
import com.intellij.psi.util.PsiMethodUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.builtins.KotlinBuiltInsNames
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.types.expressions.OperatorConventions

fun quoteKeywords(packageName: String): String = packageName.split('.').joinToString(".") { Identifier.toKotlin(it) }

fun getDefaultInitializer(property: Property): Expression? {
    val t = property.type
    val result = when {
        t.isNullable -> LiteralExpression("null")
        t is PrimitiveType -> when (t.name.name) {
            "Boolean" -> LiteralExpression("false")
            "Char" -> LiteralExpression("' '")
            "Double" -> MethodCallExpression.buildNonNull(LiteralExpression("0").assignNoPrototype(), OperatorConventions.DOUBLE.toString())
            "Float" -> MethodCallExpression.buildNonNull(LiteralExpression("0").assignNoPrototype(), OperatorConventions.FLOAT.toString())
            else -> LiteralExpression("0")
        }
        else -> null
    }
    return result?.assignNoPrototype()
}

fun shouldGenerateDefaultInitializer(searcher: ReferenceSearcher, field: PsiField)
        = field.initializer == null && (field.isVar(searcher) || !field.hasWriteAccesses(searcher, field.containingClass))

fun PsiReferenceExpression.isQualifierEmptyOrThis(): Boolean {
    val qualifier = qualifierExpression
    return qualifier == null || (qualifier is PsiThisExpression && qualifier.qualifier == null)
}

fun PsiReferenceExpression.isQualifierEmptyOrClass(psiClass: PsiClass): Boolean {
    val qualifier = qualifierExpression
    return qualifier == null || (qualifier is PsiReferenceExpression && qualifier.isReferenceTo(psiClass))
}

fun PsiElement.isInSingleLine(): Boolean {
    if (this is PsiWhiteSpace) {
        val text = text!!
        return text.indexOf('\n') < 0 && text.indexOf('\r') < 0
    }

    var child = firstChild
    while (child != null) {
        if (!child.isInSingleLine()) return false
        child = child.nextSibling
    }
    return true
}

//TODO: check for variables that are definitely assigned in constructors
fun PsiElement.getContainingMethod(): PsiMethod? {
    var context = context
    while (context != null) {
        val _context = context
        if (_context is PsiMethod) return _context
        context = _context.context
    }
    return null
}

fun PsiElement.getContainingClass(): PsiClass? {
    var context = context
    while (context != null) {
        val _context = context
        if (_context is PsiClass) return _context
        if (_context is PsiMember) return _context.containingClass
        context = _context.context
    }
    return null
}

fun PsiElement.getContainingConstructor(): PsiMethod? {
    val method = getContainingMethod()
    return if (method?.isConstructor == true) method else null
}

fun PsiMember.isConstructor(): Boolean = this is PsiMethod && this.isConstructor

fun PsiModifierListOwner.accessModifier(): String = when {
    hasModifierProperty(PsiModifier.PUBLIC) -> PsiModifier.PUBLIC
    hasModifierProperty(PsiModifier.PRIVATE) -> PsiModifier.PRIVATE
    hasModifierProperty(PsiModifier.PROTECTED) -> PsiModifier.PROTECTED
    else -> PsiModifier.PACKAGE_LOCAL
}

fun PsiMethod.isMainMethod(): Boolean = PsiMethodUtil.isMainMethod(this)

fun PsiReferenceExpression.dot(): PsiElement? = node.findChildByType(JavaTokenType.DOT)?.psi
fun PsiExpressionList.lPar(): PsiElement? = node.findChildByType(JavaTokenType.LPARENTH)?.psi
fun PsiExpressionList.rPar(): PsiElement? = node.findChildByType(JavaTokenType.RPARENTH)?.psi

fun PsiMember.isImported(file: PsiJavaFile): Boolean {
    return if (this is PsiClass) {
        val fqName = qualifiedName
        val index = fqName?.lastIndexOf('.') ?: -1
        val parentName = if (index >= 0) fqName!!.substring(0, index) else null
        file.importList?.allImportStatements?.any {
            it.importReference?.qualifiedName == (if (it.isOnDemand) parentName else fqName)
        } ?: false
    }
    else {
        containingClass != null && file.importList?.importStaticStatements?.any {
            it.resolveTargetClass() == containingClass && (it.isOnDemand || it.referenceName == name)
        } ?: false
    }
}

fun PsiExpression.isNullLiteral() = this is PsiLiteralExpression && type == PsiType.NULL

// TODO: set origin for facade classes in library
fun isFacadeClassFromLibrary(element: PsiElement?) = element is KtLightClass && element.kotlinOrigin == null

fun Converter.convertToKotlinAnalog(classQualifiedName: String?, mutability: Mutability): String? {
    if (classQualifiedName == null) return null
    return (if (mutability.isMutable(settings)) toKotlinMutableTypesMap[classQualifiedName] else null)
           ?: toKotlinTypesMap[classQualifiedName]
}

fun Converter.convertToKotlinAnalogIdentifier(classQualifiedName: String?, mutability: Mutability): Identifier? {
    val kotlinClassName = convertToKotlinAnalog(classQualifiedName, mutability) ?: return null
    return Identifier.withNoPrototype(kotlinClassName.substringAfterLast('.'))
}

val toKotlinTypesMap: Map<String, String> = mapOf(
    CommonClassNames.JAVA_LANG_OBJECT to KotlinBuiltInsNames.FqNames.any.asString(),
    CommonClassNames.JAVA_LANG_BYTE to KotlinBuiltInsNames.FqNames._byte.asString(),
    CommonClassNames.JAVA_LANG_CHARACTER to KotlinBuiltInsNames.FqNames._char.asString(),
    CommonClassNames.JAVA_LANG_DOUBLE to KotlinBuiltInsNames.FqNames._double.asString(),
    CommonClassNames.JAVA_LANG_FLOAT to KotlinBuiltInsNames.FqNames._float.asString(),
    CommonClassNames.JAVA_LANG_INTEGER to KotlinBuiltInsNames.FqNames._int.asString(),
    CommonClassNames.JAVA_LANG_LONG to KotlinBuiltInsNames.FqNames._long.asString(),
    CommonClassNames.JAVA_LANG_SHORT to KotlinBuiltInsNames.FqNames._short.asString(),
    CommonClassNames.JAVA_LANG_BOOLEAN to KotlinBuiltInsNames.FqNames._boolean.asString(),
    CommonClassNames.JAVA_LANG_ITERABLE to KotlinBuiltInsNames.FqNames.iterable.asString(),
    CommonClassNames.JAVA_UTIL_ITERATOR to KotlinBuiltInsNames.FqNames.iterator.asString(),
    CommonClassNames.JAVA_UTIL_LIST to KotlinBuiltInsNames.FqNames.list.asString(),
    CommonClassNames.JAVA_UTIL_COLLECTION to KotlinBuiltInsNames.FqNames.collection.asString(),
    CommonClassNames.JAVA_UTIL_SET to KotlinBuiltInsNames.FqNames.set.asString(),
    CommonClassNames.JAVA_UTIL_MAP to KotlinBuiltInsNames.FqNames.map.asString(),
    CommonClassNames.JAVA_UTIL_MAP_ENTRY to KotlinBuiltInsNames.FqNames.mapEntry.asString(),
    java.util.ListIterator::class.java.canonicalName to KotlinBuiltInsNames.FqNames.listIterator.asString()

)

val toKotlinMutableTypesMap: Map<String, String> = mapOf(
    CommonClassNames.JAVA_UTIL_ITERATOR to KotlinBuiltInsNames.FqNames.mutableIterator.asString(),
    CommonClassNames.JAVA_UTIL_LIST to KotlinBuiltInsNames.FqNames.mutableList.asString(),
    CommonClassNames.JAVA_UTIL_COLLECTION to KotlinBuiltInsNames.FqNames.mutableCollection.asString(),
    CommonClassNames.JAVA_UTIL_SET to KotlinBuiltInsNames.FqNames.mutableSet.asString(),
    CommonClassNames.JAVA_UTIL_MAP to KotlinBuiltInsNames.FqNames.mutableMap.asString(),
    CommonClassNames.JAVA_UTIL_MAP_ENTRY to KotlinBuiltInsNames.FqNames.mutableMapEntry.asString(),
    java.util.ListIterator::class.java.canonicalName to KotlinBuiltInsNames.FqNames.mutableListIterator.asString()
)
