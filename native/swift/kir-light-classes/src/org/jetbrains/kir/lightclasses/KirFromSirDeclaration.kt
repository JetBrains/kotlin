/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kir.lightclasses

import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.sir.SirDeclaration

internal interface KirFromSirDeclaration<S : SirDeclaration> {
    val sirDeclaration: S
    val kirSession: KirSession
}
