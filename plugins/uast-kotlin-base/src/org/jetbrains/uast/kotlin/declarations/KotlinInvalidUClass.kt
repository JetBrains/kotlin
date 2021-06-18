/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.light.LightPsiClassBuilder
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.uast.*

/**
 * implementation of [UClass] for invalid code, when it is impossible to create a [KtLightClass]
 */
class KotlinInvalidUClass(
    override val psi: PsiClass,
    givenParent: UElement?
) : AbstractKotlinUClass(givenParent), PsiClass by psi {

    constructor(name: String, context: PsiElement, givenParent: UElement?) : this(LightPsiClassBuilder(context, name), givenParent)

    override fun getContainingFile(): PsiFile? = uastParent?.getContainingUFile()?.sourcePsi

    override val sourcePsi: PsiElement? get() = null

    override val uastAnchor: UIdentifier? get() = null

    override val javaPsi: PsiClass get() = psi

    override fun getFields(): Array<UField> = emptyArray()

    override fun getInitializers(): Array<UClassInitializer> = emptyArray()

    override fun getInnerClasses(): Array<UClass> = emptyArray()

    override fun getMethods(): Array<UMethod> = emptyArray()

    override fun getSuperClass(): UClass? = null

    override fun getOriginalElement(): PsiElement? = null
}
