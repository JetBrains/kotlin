/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.library.KotlinIrSignatureVersion

internal class KlibToolArguments(
        val commandName: String,
        val libraryNameOrPath: String,
        val repository: String?,
        val printSignatures: Boolean,
        val signatureVersion: KotlinIrSignatureVersion?,
        val testMode: Boolean,
)
