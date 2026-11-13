/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.impl

import org.jetbrains.kotlin.kir.providers.KirDeclarationNamer
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirFunction

public class KirDeclarationNamerImpl : KirDeclarationNamer {
    override fun SirDeclaration.kirDeclarationName(): String = when (this) {
        is SirFunction -> name
        else -> error("Tried to calculate a KIR name for an unsupported declaration: $this")
    }
}
