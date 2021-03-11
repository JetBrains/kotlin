/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.psi.KtCallElement

internal class FirLightAnnotationForAnnotationCall(
    private val annotationCall: KtAnnotationCall,
    parent: PsiElement,
) : FirLightAbstractAnnotation(parent) {

    override fun findAttributeValue(attributeName: String?): PsiAnnotationMemberValue? =
        PsiImplUtil.findAttributeValue(this, attributeName)

    override fun findDeclaredAttributeValue(attributeName: String?) =
        PsiImplUtil.findDeclaredAttributeValue(this, attributeName)

    private val _parameterList: PsiAnnotationParameterList by lazyPub {
        FirAnnotationParameterList(this@FirLightAnnotationForAnnotationCall, annotationCall)
    }

    override fun getParameterList(): PsiAnnotationParameterList = _parameterList

    override val kotlinOrigin: KtCallElement? = annotationCall.psi

    override fun getQualifiedName(): String? = annotationCall.classId?.asSingleFqName()?.asString()

    override fun getName(): String? = qualifiedName

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightAnnotationForAnnotationCall &&
                        kotlinOrigin == other.kotlinOrigin &&
                        annotationCall == other.annotationCall)

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        basicIsEquivalentTo(this, another as? PsiAnnotation)
}