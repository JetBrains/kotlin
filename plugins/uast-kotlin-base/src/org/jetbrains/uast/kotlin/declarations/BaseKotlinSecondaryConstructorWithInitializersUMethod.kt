/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.uast.UElement

// This class was created as a workaround for KT-21617 to be the only constructor which includes `init` block
// when there is no primary constructors in the class.
// It is expected to have only one constructor of this type in a UClass.
abstract class BaseKotlinSecondaryConstructorWithInitializersUMethod(
    ktClass: KtClassOrObject?,
    psi: KtLightMethod,
    givenParent: UElement?
) : BaseKotlinConstructorUMethod(ktClass, psi, psi.kotlinOrigin, givenParent) {

    override fun getBodyExpressions(): List<KtExpression> {
        return getInitializers() + super.getBodyExpressions()
    }
}
