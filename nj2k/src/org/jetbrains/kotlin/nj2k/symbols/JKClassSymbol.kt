/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.symbols

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.nj2k.JKSymbolProvider
import org.jetbrains.kotlin.nj2k.tree.JKClass
import org.jetbrains.kotlin.psi.KtClassOrObject


sealed class JKClassSymbol : JKSymbol

class JKUniverseClassSymbol(override val symbolProvider: JKSymbolProvider) : JKClassSymbol(), JKUniverseSymbol<JKClass> {
    override lateinit var target: JKClass
    override val name: String
        get() = target.name.value
}

class JKMultiverseClassSymbol(
    override val target: PsiClass,
    override val symbolProvider: JKSymbolProvider
) : JKClassSymbol(), JKMultiverseSymbol<PsiClass>

class JKMultiverseKtClassSymbol(
    override val target: KtClassOrObject,
    override val symbolProvider: JKSymbolProvider
) : JKClassSymbol(), JKMultiverseKtSymbol<KtClassOrObject>

class JKUnresolvedClassSymbol(override val target: String) : JKClassSymbol(), JKUnresolvedSymbol