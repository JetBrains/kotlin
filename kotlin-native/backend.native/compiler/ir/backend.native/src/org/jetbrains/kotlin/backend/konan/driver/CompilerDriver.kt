/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment

internal sealed class CompilerDriver {
    abstract fun run(config: KonanConfig, environment: KotlinCoreEnvironment)
}