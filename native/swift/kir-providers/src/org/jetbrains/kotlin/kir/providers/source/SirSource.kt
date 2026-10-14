/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.source

import org.jetbrains.kotlin.kir.KirOrigin
import org.jetbrains.kotlin.sir.SirDeclaration

/**
 * An origin of a KIR declaration that was translated from [declaration].
 */
public class SirSource(
    public val declaration: SirDeclaration,
) : KirOrigin.Foreign.SourceCode {
    override fun toString(): String = "SirSource(declaration=${declaration::class.simpleName ?: "?"})"
}
