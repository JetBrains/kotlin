/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.impl

import org.jetbrains.kotlin.kir.KirAvailability
import org.jetbrains.kotlin.kir.providers.KirVisibilityChecker
import org.jetbrains.kotlin.kir.providers.utils.toKirVisibility
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirFunction

public class KirVisibilityCheckerImpl : KirVisibilityChecker {
    override fun SirDeclaration.kirAvailability(): KirAvailability = when (this) {
        is SirFunction -> KirAvailability.Available(visibility.toKirVisibility())
        else -> KirAvailability.Unavailable("Only functions are supported")
    }
}
