/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind

sealed class JsBinary(
    internal val name: String,
    private val buildKind: BuildVariantKind
)

class Executable(
    name: String,
    buildKind: BuildVariantKind,
    compilation: KotlinJsIrCompilation
) : JsBinary(
    name,
    buildKind
)