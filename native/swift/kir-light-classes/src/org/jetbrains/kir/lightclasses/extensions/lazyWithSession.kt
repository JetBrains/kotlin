/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kir.lightclasses.extensions

import org.jetbrains.kir.lightclasses.KirFromSirDeclaration
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.withSession
import org.jetbrains.kotlin.sir.SirDeclaration

internal inline fun <S : SirDeclaration, R> KirFromSirDeclaration<S>.lazyWithSession(
    crossinline block: context(KirSession) () -> R,
): Lazy<R> {
    return lazy {
        withSession(block)
    }
}

internal inline fun <S : SirDeclaration, R> KirFromSirDeclaration<S>.withSession(
    crossinline block: context(KirSession) () -> R,
): R = kirSession.withSession(block)
