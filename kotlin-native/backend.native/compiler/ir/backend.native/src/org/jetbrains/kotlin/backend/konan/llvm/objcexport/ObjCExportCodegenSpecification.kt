/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import org.jetbrains.kotlin.backend.konan.CachedLibraries
import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.backend.konan.ObjCCacheMetadata
import org.jetbrains.kotlin.konan.target.isObjCCache
import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * Decides which parts of the Objective-C export the current compilation generates, similar to
 * [org.jetbrains.kotlin.backend.konan.LlvmModuleSpecification] for Kotlin code.
 *
 * A framework generates the export of all libraries except those it takes from Objective-C export caches, and the support
 * the export needs at runtime. An Objective-C export cache generates the export of its library only, with exported names,
 * so that frameworks can link against it.
 */
internal sealed class ObjCExportCodegenSpecification {
    /**
     * Whether the adapters, file classes and categories of declarations from [library] are generated here.
     * Declarations without a library belong to the sources of the compilation.
     */
    abstract fun generatesAdaptersOf(library: KotlinLibrary?): Boolean

    /**
     * Whether the type adapter of `Any` is generated here.
     */
    abstract val generatesAnyAdapter: Boolean

    /**
     * Whether the classes of [library] get their adapters, including the reverse adapters of their methods, from another
     * object file, so that subclasses generated here can inherit them.
     */
    abstract fun hasAdaptersElsewhere(library: KotlinLibrary): Boolean

    /**
     * The library whose adapters are generated with exported names for other compilations, or `null` if adapters are
     * generated for this binary only, bound to the type infos of their classes, and collected into the adapter tables.
     */
    abstract val exportedAdaptersLibrary: KotlinLibrary?

    /**
     * The metadata of the caches whose adapters the adapter tables reference, and whose selectors this binary defines.
     */
    abstract val linkedCaches: List<ObjCCacheMetadata>

    class Framework(objCCaches: Map<KotlinLibrary, CachedLibraries.ObjCCache>) : ObjCExportCodegenSpecification() {
        private val cachedLibraries = objCCaches.keys

        override fun generatesAdaptersOf(library: KotlinLibrary?): Boolean = library !in cachedLibraries
        override val generatesAnyAdapter: Boolean get() = true
        override fun hasAdaptersElsewhere(library: KotlinLibrary): Boolean = library in cachedLibraries
        override val exportedAdaptersLibrary: KotlinLibrary? get() = null
        override val linkedCaches: List<ObjCCacheMetadata> = objCCaches.values.map { it.metadata }
    }

    class ObjCCache(
            private val library: KotlinLibrary,
            private val exportedLibraries: Set<KotlinLibrary>,
    ) : ObjCExportCodegenSpecification() {
        override fun generatesAdaptersOf(library: KotlinLibrary?): Boolean = library == this.library
        override val generatesAnyAdapter: Boolean get() = false
        override fun hasAdaptersElsewhere(library: KotlinLibrary): Boolean = library != this.library && library in exportedLibraries
        override val exportedAdaptersLibrary: KotlinLibrary get() = library
        override val linkedCaches: List<ObjCCacheMetadata> get() = emptyList()
    }

    companion object {
        fun create(config: NativeSecondStageCompilationConfig): ObjCExportCodegenSpecification =
                if (config.produce.isObjCCache) {
                    ObjCCache(config.librariesToCache.keys.single(), config.exportedLibrariesForObjCCache)
                } else {
                    Framework(config.objCCaches)
                }
    }
}
