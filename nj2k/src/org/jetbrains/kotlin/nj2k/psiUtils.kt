/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.codeInsight.generation.GenerateEqualsHelper.getEqualsSignature
import com.intellij.lang.jvm.JvmClassKind
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.ClassKind
import org.jetbrains.kotlin.j2k.ReferenceSearcher
import org.jetbrains.kotlin.j2k.isNullLiteral
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject


import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

//copied from old j2k
fun canKeepEqEq(left: PsiExpression, right: PsiExpression?): Boolean {
    if (left.isNullLiteral() || (right?.isNullLiteral() == true)) return true
    when (val type = left.type) {
        is PsiPrimitiveType, is PsiArrayType -> return true

        is PsiClassType -> {
            if (right?.type is PsiPrimitiveType) return true

            val psiClass = type.resolve() ?: return false
            if (!psiClass.hasModifierProperty(PsiModifier.FINAL)) return false
            if (psiClass.isEnum) return true

            val equalsSignature = getEqualsSignature(left.project, GlobalSearchScope.allScope(left.project))
            val equalsMethod = MethodSignatureUtil.findMethodBySignature(psiClass, equalsSignature, true)
            if (equalsMethod != null && equalsMethod.containingClass?.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT) return false

            return true
        }

        else -> return false
    }
}


internal fun PsiMember.visibility(
    referenceSearcher: ReferenceSearcher,
    assignNonCodeElements: ((JKFormattingOwner, PsiElement) -> Unit)?
): JKVisibilityModifierElement =
    modifierList?.children?.mapNotNull { child ->
        if (child !is PsiKeyword) return@mapNotNull null
        when (child.text) {
            PsiModifier.PACKAGE_LOCAL -> Visibility.INTERNAL
            PsiModifier.PRIVATE -> Visibility.PRIVATE
            PsiModifier.PROTECTED -> handleProtectedVisibility(referenceSearcher)
            PsiModifier.PUBLIC -> Visibility.PUBLIC

            else -> null
        }?.let {
            JKVisibilityModifierElement(it)
        }?.also { modifier ->
            assignNonCodeElements?.also { it(modifier, child) }
        }
    }?.firstOrNull() ?: JKVisibilityModifierElement(Visibility.INTERNAL)


fun PsiMember.modality(assignNonCodeElements: ((JKFormattingOwner, PsiElement) -> Unit)?) =
    modifierList?.children?.mapNotNull { child ->
        if (child !is PsiKeyword) return@mapNotNull null
        when (child.text) {
            PsiModifier.FINAL -> Modality.FINAL
            PsiModifier.ABSTRACT -> Modality.ABSTRACT

            else -> null
        }?.let {
            JKModalityModifierElement(it)
        }?.also { modifier ->
            assignNonCodeElements?.let { it(modifier, child) }
        }
    }?.firstOrNull() ?: JKModalityModifierElement(Modality.OPEN)


fun JvmClassKind.toJk() = when (this) {
    JvmClassKind.CLASS -> JKClass.ClassKind.CLASS
    JvmClassKind.INTERFACE ->  JKClass.ClassKind.INTERFACE
    JvmClassKind.ANNOTATION ->  JKClass.ClassKind.ANNOTATION
    JvmClassKind.ENUM ->  JKClass.ClassKind.ENUM
}

private fun PsiMember.handleProtectedVisibility(referenceSearcher: ReferenceSearcher): Visibility {
    val originalClass = containingClass ?: return Visibility.PROTECTED
    // Search for usages only in Java because java-protected member cannot be used in Kotlin from same package
    val usages = referenceSearcher.findUsagesForExternalCodeProcessing(this, searchJava = true, searchKotlin = false)

    return if (usages.any { !allowProtected(it.element, this, originalClass) })
        Visibility.PUBLIC
    else Visibility.PROTECTED
}

private fun allowProtected(element: PsiElement, member: PsiMember, originalClass: PsiClass): Boolean {
    if (element.parent is PsiNewExpression && member is PsiMethod && member.isConstructor) {
        // calls to for protected constructors are allowed only within same class or as super calls
        return element.parentsWithSelf.contains(originalClass)
    }

    return element.parentsWithSelf.filterIsInstance<PsiClass>().any { accessContainingClass ->
        if (!InheritanceUtil.isInheritorOrSelf(accessContainingClass, originalClass, true)) return@any false

        if (element !is PsiReferenceExpression) return@any true

        val qualifierExpression = element.qualifierExpression ?: return@any true

        // super.foo is allowed if 'foo' is protected
        if (qualifierExpression is PsiSuperExpression) return@any true

        val receiverType = qualifierExpression.type ?: return@any true
        val resolvedClass = PsiUtil.resolveGenericsClassInType(receiverType).element ?: return@any true

        // receiver type should be subtype of containing class
        InheritanceUtil.isInheritorOrSelf(resolvedClass, accessContainingClass, true)
    }
}

fun PsiClass.classKind(): JKClass.ClassKind =
    when {
        isAnnotationType -> JKClass.ClassKind.ANNOTATION
        isEnum -> JKClass.ClassKind.ENUM
        isInterface -> JKClass.ClassKind.INTERFACE
        else -> JKClass.ClassKind.CLASS
    }

val KtDeclaration.fqNameWithoutCompanions
    get() = generateSequence(this) { it.containingClassOrObject }
        .filter { it.safeAs<KtObjectDeclaration>()?.isCompanion() != true && it.name != null }
        .toList()
        .foldRight(containingKtFile.packageFqName) { container, acc -> acc.child(Name.identifier(container.name!!)) }

internal fun <T> runUndoTransparentActionInEdt(inWriteAction: Boolean, action: () -> T): T {
    var result: T? = null
    ApplicationManager.getApplication().invokeAndWait {
        CommandProcessor.getInstance().runUndoTransparentAction {
            result = when {
                inWriteAction -> runWriteAction(action)
                else -> action()
            }
        }
    }
    return result!!
}