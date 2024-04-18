/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.shortName
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.util.DummyLogger
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import org.jetbrains.kotlin.konan.file.File as KonanFile

interface KtObjCExportModuleNaming {
    context(KtAnalysisSession)
    fun getModuleName(module: KtModule): String?

    companion object {
        val default = KtObjCExportModuleNaming(listOf(KtKlibObjCExportModuleNaming, KtSimpleObjCExportModuleNaming))
    }
}

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtModule.getObjCKotlinModuleName(): String? {
    return cached(GetObjCKotlinModuleNameCacheKey(this)) {
        internal.moduleNaming.getModuleName(this)
    }
}

private data class GetObjCKotlinModuleNameCacheKey(private val module: KtModule)

/**
 * Combines several [implementations] to a single [KtObjCExportModuleNaming].
 * The order of [implementations] matters: The first implementation to resopnd with a module name will win.
 */
fun KtObjCExportModuleNaming(implementations: List<KtObjCExportModuleNaming>): KtObjCExportModuleNaming {
    return KtCompositeObjCExportModuleNaming(implementations)
}

internal object KtKlibObjCExportModuleNaming : KtObjCExportModuleNaming {
    context(KtAnalysisSession)
    override fun getModuleName(module: KtModule): String? {
        /*
        In this implementation, we're actually looking into the klib file, trying to resolve
        the contained manifest to get the 'shortName' or 'uniqueName'.

        This information is theoretically available already (as also used by the Analysis Api), but not yet accessible.
         */
        if (module !is KtLibraryModule) return null
        val binaryRoot = module.getBinaryRoots().singleOrNull() ?: return null
        if (!binaryRoot.isDirectory() && binaryRoot.extension != "klib") return null
        val library = runCatching { ToolingSingleFileKlibResolveStrategy.tryResolve(KonanFile(binaryRoot), DummyLogger) }
            .getOrElse { error -> error.printStackTrace(); return null } ?: return null
        return library.shortName ?: library.uniqueName
    }
}

internal object KtSimpleObjCExportModuleNaming : KtObjCExportModuleNaming {
    context(KtAnalysisSession)
    override fun getModuleName(module: KtModule): String? {
        return when (module) {
            is KtSourceModule -> module.stableModuleName ?: module.moduleName
            is KtLibraryModule -> module.libraryName
            else -> null
        }
    }
}

internal class KtCompositeObjCExportModuleNaming(private val implementations: List<KtObjCExportModuleNaming>) : KtObjCExportModuleNaming {
    context(KtAnalysisSession) override fun getModuleName(module: KtModule): String? {
        return implementations.firstNotNullOfOrNull { implementation -> implementation.getModuleName(module) }
    }
}
