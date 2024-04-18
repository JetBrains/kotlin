/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.project.structure.KtModule

fun interface KtObjCExportModuleClassifier {
    /**
     * Will return true if the module is considered 'exported' in the build.
     * e.g. the Gradle project building the framework should always be considered as 'exported'.
     * However, dependencies of such a Gradle project (e.g. a 'utils' module) can either be declared
     * as 'exported' or not exported.
     *
     * Exported modules will get their full API surface includced in the final framework.
     * Non-exported modules will get an additional module 'string' attached to exported classifiers.
     */
    fun isExported(module: KtModule): Boolean

    companion object {
        val default: KtObjCExportModuleClassifier = KtObjCExportDefaultModuleClassifier

    }
}

/**
 * See [KtObjCExportDefaultModuleClassifier.isExported]:
 * Note: This method will be cached.
 */
internal fun KtObjCExportSession.isExported(module: KtModule): Boolean = cached(IsExportedCacheKey(module)) {
    internal.moduleClassifier.isExported(module)
}

data class IsExportedCacheKey(val module: KtModule)

private object KtObjCExportDefaultModuleClassifier : KtObjCExportModuleClassifier {
    override fun isExported(module: KtModule): Boolean {
        return true
    }
}

