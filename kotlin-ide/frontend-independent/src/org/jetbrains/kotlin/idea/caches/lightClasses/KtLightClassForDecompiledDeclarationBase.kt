/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.lightClasses

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiExtensibleClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtClassOrObject

abstract class KtLightClassForDecompiledDeclarationBase(
    override val clsDelegate: PsiClass,
    clsParent: PsiElement,
    final override val kotlinOrigin: KtClassOrObject?
) : KtLightElementBase(clsParent), PsiClass, KtLightClass, PsiExtensibleClass