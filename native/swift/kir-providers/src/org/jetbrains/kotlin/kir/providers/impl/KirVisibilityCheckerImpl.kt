/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.impl

import org.jetbrains.kotlin.kir.KirAvailability
import org.jetbrains.kotlin.kir.providers.KirVisibilityChecker
import org.jetbrains.kotlin.kir.providers.utils.toKirVisibility
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirInit
import org.jetbrains.kotlin.sir.SirVisibility

public class KirVisibilityCheckerImpl : KirVisibilityChecker {
    override fun SirDeclaration.kirAvailability(): KirAvailability = when (this) {
        is SirClass, is SirFunction, is SirInit ->
            if (visibility == SirVisibility.PUBLIC) {
                KirAvailability.Available(visibility.toKirVisibility())
            } else {
                KirAvailability.Unavailable("Only public declarations are visible outside of the module")
            }
        else -> KirAvailability.Unavailable("Only classes, functions and initializers are supported")
    }
}
