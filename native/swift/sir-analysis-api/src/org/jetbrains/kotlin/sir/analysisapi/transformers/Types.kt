/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.analysisapi.transformers

import org.jetbrains.kotlin.sir.SirKotlinOrigin

internal data class AAParameter(
    override val name: String,
    override val type: SirKotlinOrigin.Type
) : SirKotlinOrigin.Parameter

internal data class AAKotlinType(
    override val name: String
) : SirKotlinOrigin.Type
