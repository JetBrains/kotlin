/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.mock

import org.jetbrains.kotlin.sir.KotlinFunction
import org.jetbrains.kotlin.sir.KotlinType
import org.jetbrains.kotlin.sir.KotlinParameter

data class MockFunction(
    override val fqName: List<String>,
    override val parameters: List<KotlinParameter>,
    override val returnType: KotlinType,
) : KotlinFunction

data class MockParameter(
    override val name: String,
    override val type: KotlinType,
) : KotlinParameter

data class MockKotlinType(
    override val name: String
) : KotlinType
