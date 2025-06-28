/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.FrontendContext
import org.jetbrains.kotlin.backend.FrontendServices
import org.jetbrains.kotlin.backend.NativeFrontendConfig
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportLazy
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportLazyImpl
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportProblemCollector
import org.jetbrains.kotlin.backend.konan.objcexport.dumpObjCHeader
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtFile

internal fun StorageComponentContainer.initContainer(config: NativeFrontendConfig, configuration: CompilerConfiguration) {
    useImpl<FrontendServices>()

    if (!configuration.get(KonanConfigKeys.EMIT_LAZY_OBJC_HEADER_FILE).isNullOrEmpty()) {
        useImpl<ObjCExportLazyImpl>()
        useInstance(object : ObjCExportProblemCollector {
            override fun reportWarning(text: String) {}
            override fun reportWarning(declaration: DeclarationDescriptor, text: String) {}
            override fun reportError(text: String) {}
            override fun reportError(declaration: DeclarationDescriptor, text: String) {}
            override fun reportException(throwable: Throwable) = throw throwable
        })

        useInstance(object : ObjCExportLazy.Configuration {
            override val frameworkName: String
                get() = config.fullExportedNamePrefix

            override fun isIncluded(moduleInfo: ModuleInfo): Boolean = true

            override fun getCompilerModuleName(moduleInfo: ModuleInfo): String {
                TODO()
            }

            override val objcGenerics: Boolean
                get() = configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)

            override val disableSwiftMemberNameMangling: Boolean
                get() = configuration.getBoolean(BinaryOptions.objcExportDisableSwiftMemberNameMangling)

            override val unitSuspendFunctionExport: UnitSuspendFunctionObjCExport
                get() = TODO()

            override val ignoreInterfaceMethodCollisions: Boolean
                get() = configuration.getBoolean(BinaryOptions.objcExportIgnoreInterfaceMethodCollisions)
        })
    }
}

internal fun ComponentProvider.postprocessComponents(context: FrontendContext, files: Collection<KtFile>) {
    context.frontendServices = this.get<FrontendServices>()

    context.configuration.get(KonanConfigKeys.EMIT_LAZY_OBJC_HEADER_FILE)?.takeIf { it.isNotEmpty() }?.let {
        this.get<ObjCExportLazy>().dumpObjCHeader(files, it, context.shouldExportKDoc())
    }
}