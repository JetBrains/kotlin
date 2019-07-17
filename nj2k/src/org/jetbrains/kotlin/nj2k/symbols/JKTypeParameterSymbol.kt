/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.symbols

import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.nj2k.JKSymbolProvider
import org.jetbrains.kotlin.nj2k.tree.JKTypeParameter
import org.jetbrains.kotlin.nj2k.tree.JKTypeParameterListOwner
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class JKTypeParameterSymbol : JKSymbol {
    abstract val index: Int
}

class JKMultiverseTypeParameterSymbol(
    override val target: PsiTypeParameter,
    override val symbolProvider: JKSymbolProvider
) : JKTypeParameterSymbol(), JKMultiverseSymbol<PsiTypeParameter> {
    override val index: Int
        get() = target.index
}

class JKUniverseTypeParameterSymbol(
    override val symbolProvider: JKSymbolProvider
) : JKTypeParameterSymbol(), JKUniverseSymbol<JKTypeParameter> {
    override val index: Int
        get() = declaredIn?.safeAs<JKTypeParameterListOwner>()?.typeParameterList?.typeParameters?.indexOf(target) ?: -1
    override lateinit var target: JKTypeParameter
}

class JKMultiverseKtTypeParameterSymbol(
    override val target: KtTypeParameter,
    override val symbolProvider: JKSymbolProvider
) : JKTypeParameterSymbol(), JKMultiverseKtSymbol<KtTypeParameter> {
    override val index: Int
        get() = target.getParentOfType<KtTypeParameterListOwner>(strict = false)?.typeParameters?.indexOf(target) ?: -1
}