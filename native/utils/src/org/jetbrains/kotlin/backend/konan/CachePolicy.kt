/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

/**
 * Describes how a compiler option interacts with the Kotlin/Native compiler caches.
 * The caches are built by separate compilations spawned by the compiler, and the cache directory
 * is selected by the cache flavor computed from the options affecting the content of the caches.
 */
enum class CachePolicy {
    /**
     * Isn't passed to the cache builds: either only matters for the top-level compilation (linking, ObjC export,
     * the runtime globals overridden in the final binary, etc.), or is set up for the cache builds explicitly.
     */
    NOT_PROPAGATED,

    /** Is passed to the cache builds, but doesn't affect the content of the caches. */
    PROPAGATED,

    /**
     * Affects the runtime, which is compiled into the stdlib cache. Is passed to the cache builds
     * and is a part of the flavor of the caches of the distribution libraries.
     */
    AFFECTS_RUNTIME,

    /** Affects the code generated for any library. Is passed to the cache builds and is a part of all cache flavors. */
    AFFECTS_CODEGEN,

    /** The caches can't be used when the option is set. */
    INCOMPATIBLE_WITH_CACHES,
}
