/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedConstantValue
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSimpleConstantValue
import org.jetbrains.kotlin.psi.KtElement

internal class FirAnnotationParameterList(
    parent: FirLightAbstractAnnotation,
    private val annotationCall: KtAnnotationCall,
) : KtLightElementBase(parent), PsiAnnotationParameterList {

    private val _attributes: Array<PsiNameValuePair> by lazyPub {
        annotationCall.arguments.map {
            FirNameValuePairForAnnotationArgument(it, this)
        }.toTypedArray()
    }

    override fun getAttributes(): Array<PsiNameValuePair> = _attributes

    override val kotlinOrigin: KtElement? get() = null

    //TODO: EQ GHC EQIV
}

private class FirNameValuePairForAnnotationArgument(
    private val constantValue: KtNamedConstantValue,
    parent: PsiElement
) : KtLightElementBase(parent), PsiNameValuePair {

    override val kotlinOrigin: KtElement? get() = null

    private val _value by lazyPub {
        (constantValue.expression as? KtSimpleConstantValue<*>)?.createPsiLiteral(this)
    }

    override fun setValue(p0: PsiAnnotationMemberValue) = cannotModify()

    private val _nameIdentifier: PsiIdentifier by lazyPub {
        LightIdentifier(parent.manager, constantValue.name)
    }

    override fun getNameIdentifier(): PsiIdentifier = _nameIdentifier

    override fun getValue(): PsiAnnotationMemberValue? = _value

    override fun getLiteralValue(): String? = (value as? PsiLiteralExpression)?.value?.toString()

    override fun getName(): String = constantValue.name
}