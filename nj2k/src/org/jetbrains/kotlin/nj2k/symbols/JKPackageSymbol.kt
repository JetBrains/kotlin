/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.symbols

import com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.nj2k.JKSymbolProvider


sealed class JKPackageSymbol : JKSymbol {
    abstract override val target: PsiPackage
}

class JKMultiversePackageSymbol(
    override val target: PsiPackage,
    override val symbolProvider: JKSymbolProvider
) : JKPackageSymbol(), JKMultiverseSymbol<PsiPackage>