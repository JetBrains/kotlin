/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.shortName
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.util.DummyLogger
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import org.jetbrains.kotlin.konan.file.File as KonanFile

interface KtObjCExportModuleNaming {

    fun KaSession.getModuleName(module: KaModule): String?

    companion object {
        val default = KtObjCExportModuleNaming(listOf(KtKlibObjCExportModuleNaming, KtSimpleObjCExportModuleNaming))
    }
}

internal fun ObjCExportContext.getObjCKotlinModuleName(module: KaModule): String? {
    return exportSession.cached(GetObjCKotlinModuleNameCacheKey(module)) {
        with(exportSession.internal.moduleNaming) {
            analysisSession.getModuleName(module)
        }
    }
}

private data class GetObjCKotlinModuleNameCacheKey(private val module: KaModule)

/**
 * Combines several [implementations] to a single [KtObjCExportModuleNaming].
 * The order of [implementations] matters: The first implementation to resopnd with a module name will win.
 */
fun KtObjCExportModuleNaming(implementations: List<KtObjCExportModuleNaming>): KtObjCExportModuleNaming {
    return KtCompositeObjCExportModuleNaming(implementations)
}

internal object KtKlibObjCExportModuleNaming : KtObjCExportModuleNaming {
    override fun KaSession.getModuleName(module: KaModule): String? {
        /*
        In this implementation, we're actually looking into the klib file, trying to resolve
        the contained manifest to get the 'shortName' or 'uniqueName'.

        This information is theoretically available already (as also used by the Analysis Api), but not yet accessible.
         */
        if (module !is KaLibraryModule) return null
        val binaryRoot = module.binaryRoots.singleOrNull() ?: return null
        if (!binaryRoot.isDirectory() && binaryRoot.extension != "klib") return null
        val library = runCatching { ToolingSingleFileKlibResolveStrategy.tryResolve(KonanFile(binaryRoot), DummyLogger) }
            .getOrElse { error -> error.printStackTrace(); return null } ?: return null
        return library.shortName ?: library.uniqueName
    }
}

internal object KtSimpleObjCExportModuleNaming : KtObjCExportModuleNaming {
    @OptIn(KaExperimentalApi::class)
    override fun KaSession.getModuleName(module: KaModule): String? {
        return when (module) {
            is KaSourceModule -> module.stableModuleName ?: module.name
            is KaLibraryModule -> module.libraryName
            else -> null
        }
    }
}

internal class KtCompositeObjCExportModuleNaming(private val implementations: List<KtObjCExportModuleNaming>) : KtObjCExportModuleNaming {
    override fun KaSession.getModuleName(module: KaModule): String? {
        return implementations.firstNotNullOfOrNull { implementation ->
            with(implementation) {
                getModuleName(module)
            }
        }
    }
}
