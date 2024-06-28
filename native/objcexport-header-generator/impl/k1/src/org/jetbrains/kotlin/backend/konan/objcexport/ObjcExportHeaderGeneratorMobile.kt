/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class ObjcExportHeaderGeneratorMobile internal constructor(
    moduleDescriptors: List<ModuleDescriptor>,
    mapper: ObjCExportMapper,
    namer: ObjCExportNamer,
    problemCollector: ObjCExportProblemCollector,
    objcGenerics: Boolean,
    private val restrictToLocalModules: Boolean,
) : ObjCExportHeaderGenerator(moduleDescriptors, mapper, namer, objcGenerics, problemCollector) {

    companion object {
        fun createInstance(
            configuration: ObjCExportLazy.Configuration,
            problemCollector: ObjCExportProblemCollector,
            builtIns: KotlinBuiltIns,
            moduleDescriptors: List<ModuleDescriptor>,
            deprecationResolver: DeprecationResolver? = null,
            local: Boolean = false,
            restrictToLocalModules: Boolean = false,
        ): ObjCExportHeaderGenerator {
            val mapper = ObjCExportMapper(deprecationResolver, local, configuration.unitSuspendFunctionExport)
            val namerConfiguration = createNamerConfiguration(configuration)
            val namer = ObjCExportNamerImpl(namerConfiguration, builtIns, mapper, problemCollector, local)

            return ObjcExportHeaderGeneratorMobile(
                moduleDescriptors,
                mapper,
                namer,
                problemCollector,
                configuration.objcGenerics,
                restrictToLocalModules
            )
        }
    }

    override fun shouldTranslateExtraClass(descriptor: ClassDescriptor): Boolean =
        !restrictToLocalModules || descriptor.module in moduleDescriptors
}
