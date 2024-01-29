/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.mock

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirKotlinOrigin
import org.jetbrains.kotlin.sir.SirOrigin

data class MockFunction(
    override val fqName: FqName,
    override val parameters: List<SirKotlinOrigin.Parameter>,
    override val returnType: SirKotlinOrigin.Type,
    override val documentation: SirKotlinOrigin.Documentation? = null,
) : SirKotlinOrigin.Function

data class MockParameter(
    override val name: String,
    override val type: SirKotlinOrigin.Type,
) : SirKotlinOrigin.Parameter

data class MockKotlinType(
    override val name: String
) : SirKotlinOrigin.Type

data class MockDocumentation(override val content: String) : SirKotlinOrigin.Documentation
