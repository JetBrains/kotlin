/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.tree

import com.intellij.psi.PsiCatchSection
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.JKSymbolProvider
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.conversions.resolveFqName
import org.jetbrains.kotlin.j2k.tree.impl.JKClassSymbol
import org.jetbrains.kotlin.j2k.tree.impl.JKClassTypeImpl
import org.jetbrains.kotlin.j2k.tree.impl.toJkType
import org.jetbrains.kotlin.name.ClassId


fun JKExpression.type(context: ConversionContext): JKType =
    when (this) {
        is JKLiteralExpression -> this.type.toJkType(context.symbolProvider)
        else -> TODO(this.toString())
    }

fun ClassId.toKtClassType(
    symbolProvider: JKSymbolProvider,
    nullability: Nullability = Nullability.Default,
    context: PsiElement = symbolProvider.symbolsByPsi.keys.first()
): JKType {
    val typeSymbol = symbolProvider.provideDirectSymbol(resolveFqName(this, context)!!) as JKClassSymbol
    return JKClassTypeImpl(typeSymbol, emptyList(), nullability)
}

