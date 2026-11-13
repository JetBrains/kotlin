/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.utils

import org.jetbrains.kotlin.kir.KirVisibility
import org.jetbrains.kotlin.sir.SirVisibility

/**
 * Maps Swift visibility to the closest Kotlin one.
 */
public fun SirVisibility.toKirVisibility(): KirVisibility = when (this) {
    SirVisibility.PUBLIC -> KirVisibility.PUBLIC
    SirVisibility.PACKAGE, SirVisibility.INTERNAL -> KirVisibility.INTERNAL
    SirVisibility.FILEPRIVATE, SirVisibility.PRIVATE -> KirVisibility.PRIVATE
}
