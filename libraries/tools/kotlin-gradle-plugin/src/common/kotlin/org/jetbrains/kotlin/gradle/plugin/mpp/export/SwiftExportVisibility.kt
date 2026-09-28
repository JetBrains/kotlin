/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.export

import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl

/**
 * How much of a dependency ends up in the generated Swift API.
 *
 * By default a direct `api` dependency is fully exported and everything else only as far as the exported
 * modules refer to it. A declared visibility overrides that.
 *
 * This API is experimental and may change in future versions.
 *
 * @since 2.5.0
 */
@ExperimentalSwiftExportDsl
enum class SwiftExportVisibility {
    /**
     * The whole public API is translated, as for a direct `api` dependency.
     *
     * The dependency must already be in the Swift Export graph; this doesn't add it.
     */
    EXPOSED,

    /**
     * Only empty stubs are generated for the types other modules refer to; members and unreferenced declarations
     * are dropped. The dependency stays in the Swift Export graph, so declarations that mention its types are
     * still exported.
     */
    HIDDEN,
}
