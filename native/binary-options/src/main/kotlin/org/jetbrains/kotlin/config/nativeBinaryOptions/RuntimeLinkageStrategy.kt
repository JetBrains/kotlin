/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

/**
 * To avoid combinatorial explosion, we split runtime into several LLVM modules.
 * This approach might cause performance degradation in some compilation modes because there is no LTO between runtime modules.
 * RuntimeLinkageStrategy allows to choose the way we link runtime into final application or cache and mitigate the problem above.
 */
enum class RuntimeLinkageStrategy {
    /** Link runtime "as is", without any optimizations. Doable for "release" because LTO in this mode is quite aggressive. */
    Raw,

    /** Links all runtime modules into a single one and optimizes it. */
    Optimize
}