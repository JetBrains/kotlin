/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


fun KtExpression.asProperty(): KtProperty? =
    (this as? KtNameReferenceExpression)
        ?.mainReference
        ?.resolve() as? KtProperty

fun KtExpression.unpackedReferenceToProperty(): KtProperty? =
    when (this) {
        is KtDotQualifiedExpression ->
            if (receiverExpression is KtThisExpression) selectorExpression as? KtNameReferenceExpression
            else null
        is KtNameReferenceExpression -> this
        else -> null
    }?.references
        ?.firstOrNull { it is KtSimpleNameReference }
        ?.resolve() as? KtProperty


fun KtDeclaration.type() =
    (resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType

fun KtElement.topLevelContainingClassOrObject(): KtClassOrObject? =
    generateSequence(getStrictParentOfType<KtClassOrObject>()) {
        it.getStrictParentOfType()
    }.lastOrNull()

fun KtReferenceExpression.resolve() =
    mainReference.resolve()

fun KtPsiFactory.createGetter(body: KtExpression?): KtPropertyAccessor {
    val property =
        createProperty("val x get" + if (body == null) "" else if (body is KtBlockExpression) "() { return 1 }" else "() = 1")
    val getter = property.getter!!
    val bodyExpression = getter.bodyExpression

    bodyExpression?.replace(body!!)
    return getter
}

fun KtPsiFactory.createSetter(body: KtExpression?, fieldName: String): KtPropertyAccessor {
    val property = when (body) {
        null -> createProperty("var x = 1\n  get() = 1\n set")
        is KtBlockExpression -> createProperty("var x get() = 1\nset($fieldName) {\n field = $fieldName\n }")
        else -> createProperty("var x get() = 1\nset($fieldName) = TODO()")
    }
    val setter = property.setter!!
    if (body != null) {
        setter.bodyExpression?.replace(body)
    }
    return setter
}

fun KtClassOrObject.parentClassForCompanionOrThis(): KtClassOrObject =
    if (safeAs<KtObjectDeclaration>()?.isCompanion() == true)
        getStrictParentOfType() ?: this
    else this

fun KtElement.hasUsagesOutsideOf(inElement: KtElement, outsideElements: List<KtElement>): Boolean =
    ReferencesSearch.search(this, LocalSearchScope(inElement)).any { reference ->
        outsideElements.none { it.isAncestor(reference.element) }
    }

fun String.escaped() =
    if (this in keywords || '$' in this) "`$this`"
    else this

private val keywords = KtTokens.KEYWORDS.types.map { (it as KtKeywordToken).value }.toSet()
