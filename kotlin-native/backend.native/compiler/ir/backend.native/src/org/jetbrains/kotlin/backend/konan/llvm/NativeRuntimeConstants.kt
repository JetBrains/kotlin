/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

/**
 * Set of variables used to customize runtime behavior.
 *
 * Note that there are two ways of defining variables for runtime usage. The other way can is located within
 * [NativeRuntimeOverridableConstants] file.
 *
 * Those variables are **eligible** for runtime optimizations and fixed at the point of compiling caches.
 * So use this way for variables, which are **heavily** used on performance-critical passes
 * or **will significantly increase code size**, if not eliminated.
 *
 * When adding a new variable, please remember to adjust cache-disabling rules and add value to `CompilerGenerated.cpp` for tests.
 *
 * Forward declarations of those variables within runtime code can be found in `CompilerConstants.hpp` file.
 */
object NativeRuntimeConstants {
    const val NEED_DEBUG_INFO: String = "Kotlin_needDebugInfo"
    const val RUNTIME_ASSERTS_MODE: String = "Kotlin_runtimeAssertsMode"
    const val DISABLE_MMAP: String = "Kotlin_disableMmap"
    const val RUNTIME_LOGS_ENABLED: String = "Kotlin_runtimeLogsEnabled"
    const val CONCURRENT_WEAK_SWEEP: String = "Kotlin_concurrentWeakSweep"
    const val GC_MARK_SINGLE_THREADED: String = "Kotlin_gcMarkSingleThreaded"
    const val FIXED_BLOCK_PAGE_SIZE: String = "Kotlin_fixedBlockPageSize"
    const val PAGED_ALLOCATOR: String = "Kotlin_pagedAllocator"
}
