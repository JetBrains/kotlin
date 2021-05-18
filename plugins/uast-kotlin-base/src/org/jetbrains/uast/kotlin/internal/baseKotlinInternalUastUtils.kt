/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import java.util.function.Supplier

typealias BaseResolveProviderServiceSupplier = Supplier<BaseKotlinUastResolveProviderService>

fun <T> lz(initializer: () -> T) =
    lazy(LazyThreadSafetyMode.SYNCHRONIZED, initializer)

inline fun <reified T : UDeclaration, reified P : PsiElement> unwrap(element: P): P {
    val unwrapped = if (element is T) element.javaPsi else element
    assert(unwrapped !is UElement)
    return unwrapped as P
}

fun unwrapFakeFileForLightClass(file: PsiFile): PsiFile = (file as? FakeFileForLightClass)?.ktFile ?: file

fun getKotlinMemberOrigin(element: PsiElement?): KtDeclaration? {
    (element as? KtLightMember<*>)?.lightMemberOrigin?.auxiliaryOriginalElement?.let { return it }
    (element as? KtLightElement<*, *>)?.kotlinOrigin?.let { return it as? KtDeclaration }
    return null
}
