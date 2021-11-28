/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

internal object DependencyLibrariesOptionType : DependenciesLibrariesSetOptionType(
    mandatory = false,
    alias = "dependency-libraries",
    description = "';' separated list of klib file paths that can be used as dependency"
)
