/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.tree

import org.jetbrains.kotlin.j2k.JKSymbolProvider
import org.jetbrains.kotlin.j2k.conversions.resolveFqName
import org.jetbrains.kotlin.j2k.tree.impl.JKClassSymbol
import org.jetbrains.kotlin.j2k.tree.impl.JKClassTypeImpl
import org.jetbrains.kotlin.name.ClassId

fun kotlinTypeByName(name: String, symbolProvider: JKSymbolProvider): JKClassType {
    val symbol =
        symbolProvider.provideDirectSymbol(
            resolveFqName(ClassId.fromString(name), symbolProvider.symbolsByPsi.keys.first())!!
        ) as JKClassSymbol
    return JKClassTypeImpl(symbol, emptyList())
}