/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.tree.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.tree.JKElement
import org.jetbrains.kotlin.j2k.tree.JKSymbol


abstract class JKBindableSymbol<E : JKElement> : JKSymbol<E> {
    final override lateinit var element: E
        private set

    val isBound get() = ::element.isInitialized

    fun bind(new: E) {
        element = new
    }
}

class DelayedPsiSymbol<E : JKElement>(val psi: PsiElement) : JKBindableSymbol<E>()