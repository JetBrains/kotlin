/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.types.Variance

public val Variance.exportedVariance: ExportedVariance
    get() = when (this) {
        Variance.INVARIANT -> ExportedVariance.INVARIANT
        Variance.IN_VARIANCE -> ExportedVariance.CONTRAVARIANT
        Variance.OUT_VARIANCE -> ExportedVariance.COVARIANT
    }
