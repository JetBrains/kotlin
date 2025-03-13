/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirDeclarationContainer
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.util.swiftFqNameOrNull
import org.jetbrains.kotlin.sir.util.swiftParentNamePrefix
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.withSessions


internal inline fun <reified S : KaNamedClassSymbol, reified T> T.relocatedDeclarationNamePrefix(): String? where T : SirFromKtSymbol<S>, T : SirDeclaration =
    withSessions {
        ktSymbol.getOriginalSirParent(this@withSessions).let { originalParent ->
            (originalParent != parent && originalParent !is SirModule).ifTrue {
                (originalParent as? SirDeclarationContainer)?.swiftFqNameOrNull?.let {
                    it.removePrefix(it.commonPrefixWith(this@relocatedDeclarationNamePrefix.swiftParentNamePrefix ?: ""))
                }?.removePrefix(".")?.replace(".", "_")
            }?.let { "_${it}_" }
        }
    }
