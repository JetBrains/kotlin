/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.symbols


import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.JKSymbolProvider
import org.jetbrains.kotlin.nj2k.tree.JKType
import org.jetbrains.kotlin.nj2k.tree.JKVariable
import org.jetbrains.kotlin.nj2k.tree.impl.JKClassTypeImpl
import org.jetbrains.kotlin.nj2k.tree.toJK
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.containingClass

sealed class JKFieldSymbol : JKSymbol {
    abstract val fieldType: JKType?
}

class JKUniverseFieldSymbol(override val symbolProvider: JKSymbolProvider) : JKFieldSymbol(), JKUniverseSymbol<JKVariable> {
    override val fieldType: JKType
        get() = target.type.type

    override lateinit var target: JKVariable
}

class JKMultiverseFieldSymbol(
    override val target: PsiVariable,
    override val symbolProvider: JKSymbolProvider
) : JKFieldSymbol(), JKMultiverseSymbol<PsiVariable> {
    override val fieldType: JKType
        get() = target.type.toJK(symbolProvider)
}

class JKMultiversePropertySymbol(
    override val target: KtCallableDeclaration,
    override val symbolProvider: JKSymbolProvider
) : JKFieldSymbol(), JKMultiverseKtSymbol<KtCallableDeclaration> {
    override val fieldType: JKType?
        get() = target.typeReference?.toJK(symbolProvider)
}

class JKMultiverseKtEnumEntrySymbol(
    override val target: KtEnumEntry,
    override val symbolProvider: JKSymbolProvider
) : JKFieldSymbol(), JKMultiverseKtSymbol<KtEnumEntry> {
    override val fieldType: JKType?
        get() = JKClassTypeImpl(
            symbolProvider.provideDirectSymbol(target.containingClass()!!) as JKClassSymbol,
            emptyList(),
            Nullability.NotNull
        )
}

class JKUnresolvedField(
    override val target: String,
    private val symbolProvider: JKSymbolProvider
) : JKFieldSymbol(), JKUnresolvedSymbol {
    override val fieldType: JKType
        get() =
            JKClassTypeImpl(symbolProvider.provideClassSymbol(KotlinBuiltIns.FQ_NAMES.nothing.toSafe()), emptyList())
}

