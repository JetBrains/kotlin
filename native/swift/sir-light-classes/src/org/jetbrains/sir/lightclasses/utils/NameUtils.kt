/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirDeclarationContainer
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.providers.getOriginalSirParent
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.swiftFqNameOrNull
import org.jetbrains.kotlin.sir.util.swiftParentNamePrefix
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import kotlin.math.max


internal inline fun <reified S : KaNamedClassSymbol, reified T> T.relocatedDeclarationNamePrefix(): String? where T : SirFromKtSymbol<S>, T : SirDeclaration =
    sirSession.withSessions {
        ktSymbol.getOriginalSirParent().let { originalParent ->
            (originalParent != parent && originalParent !is SirModule).ifTrue {
                (originalParent as? SirDeclarationContainer)?.swiftFqNameOrNull?.let {
                    it.removePrefix(it.commonPrefixWith(this@relocatedDeclarationNamePrefix.swiftParentNamePrefix ?: ""))
                }?.removePrefix(".")?.replace(".", "_")
            }?.let { "_${it}_" }
        }
    }

internal fun decapitalizeNameSemantically(name: String): String {
    val length = name.length
    val start = name.indexOfFirst { it.isLetter() }.takeIf { it != -1 } ?: return name

    val end = (start until length).firstOrNull() { name[it].isLowerCase() }
        ?.let { it.takeIf { it != start } ?: return name }
        ?.let { max(start + 1, it - 1) }
        ?: length

    return name.substring(0, start) + name.substring(start, end).lowercase() + name.substring(end)
}