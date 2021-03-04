/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportLazy
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportLazyImpl
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportProblemCollector
import org.jetbrains.kotlin.backend.konan.objcexport.dumpObjCHeader
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver

internal fun StorageComponentContainer.initContainer(config: KonanConfig) {
    useImpl<FrontendServices>()

    if (config.configuration.get(KonanConfigKeys.EMIT_LAZY_OBJC_HEADER_FILE) != null) {
        useImpl<ObjCExportLazyImpl>()
        useInstance(object : ObjCExportProblemCollector {
            override fun reportWarning(text: String) {}
            override fun reportWarning(method: FunctionDescriptor, text: String) {}
            override fun reportException(throwable: Throwable) = throw throwable
        })

        useInstance(object : ObjCExportLazy.Configuration {
            override val frameworkName: String
                get() = config.moduleId

            override fun isIncluded(moduleInfo: ModuleInfo): Boolean = true

            override fun getCompilerModuleName(moduleInfo: ModuleInfo): String {
                TODO()
            }

            override val objcGenerics: Boolean
                get() = config.configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)
        })
    }
}

internal fun ComponentProvider.postprocessComponents(context: Context, files: Collection<KtFile>) {
    context.frontendServices = this.get<FrontendServices>()

    context.config.configuration.get(KonanConfigKeys.EMIT_LAZY_OBJC_HEADER_FILE)?.let {
        this.get<ObjCExportLazy>().dumpObjCHeader(files, it, context.shouldExportKDoc())
    }
}

class FrontendServices(val deprecationResolver: DeprecationResolver)