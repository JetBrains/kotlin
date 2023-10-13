/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion

internal fun KotlinIrSignatureVersion?.getMostSuitableSignatureRenderer() = when (this) {
    KotlinIrSignatureVersion.V1 -> IdSignatureRenderer.LEGACY
    null, KotlinIrSignatureVersion.V2 -> IdSignatureRenderer.DEFAULT
    else -> error("Unsupported signature version: $number")
}
