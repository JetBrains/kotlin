/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.utils

import org.jetbrains.kotlin.kir.KirModality
import org.jetbrains.kotlin.sir.SirModality

/**
 * Maps Swift modality to the Kotlin one.
 */
public fun SirModality.toKirModality(): KirModality = when (this) {
    SirModality.OPEN -> KirModality.OPEN
    SirModality.FINAL -> KirModality.FINAL
    SirModality.UNSPECIFIED -> KirModality.UNSPECIFIED
}
