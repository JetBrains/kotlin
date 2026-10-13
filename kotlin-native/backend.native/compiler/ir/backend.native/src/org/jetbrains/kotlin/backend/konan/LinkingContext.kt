/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.BasicNativeBackendPhaseContext
import org.jetbrains.kotlin.util.PerformanceManager

/**
 * Context of the link phases of a compilation job. They only work with files, so it carries no LLVM state.
 */
internal class LinkingContext(
        config: NativeSecondStageCompilationConfig,
        override val performanceManager: PerformanceManager?,
) : BasicNativeBackendPhaseContext(config)
