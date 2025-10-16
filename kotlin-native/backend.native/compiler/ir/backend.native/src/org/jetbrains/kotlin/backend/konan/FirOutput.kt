/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.fir.pipeline.FirResult

sealed class FirOutput {
    object ShouldNotGenerateCode : FirOutput()

    data class Full(val firResult: FirResult) : FirOutput()
}