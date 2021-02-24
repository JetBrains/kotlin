/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

internal object InputLibrariesOptionType : LibrariesSetOptionType(
    mandatory = true,
    alias = "input-libraries",
    description = "';' separated list of klib file paths that will get commonized"
)
