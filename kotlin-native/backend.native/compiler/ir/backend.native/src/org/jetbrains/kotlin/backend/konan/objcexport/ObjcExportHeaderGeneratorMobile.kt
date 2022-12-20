package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal class ObjcExportHeaderGeneratorMobile internal constructor(
        moduleDescriptors: List<ModuleDescriptor>,
        moduleTranslationConfig: ModuleTranslationConfig,
        mapper: ObjCExportMapper,
        namer: ModuleObjCExportNamer,
        problemCollector: ObjCExportProblemCollector,
        objcGenerics: Boolean,
        private val restrictToLocalModules: Boolean,
        sharedState: ObjCExportSharedState,
) : ObjCExportHeaderGenerator(moduleTranslationConfig, moduleDescriptors, mapper, namer, namer, objcGenerics, problemCollector, sharedState) {

    internal companion object {
        fun createInstance(
                configuration: ObjCExportLazy.Configuration,
                problemCollector: ObjCExportProblemCollector,
                builtIns: KotlinBuiltIns,
                moduleTranslationConfig: ModuleTranslationConfig,
                moduleDescriptors: List<ModuleDescriptor>,
                deprecationResolver: DeprecationResolver? = null,
                local: Boolean = false,
                restrictToLocalModules: Boolean = false,
                sharedState: ObjCExportSharedState,
        ): ObjCExportHeaderGenerator {
            val mapper = ObjCExportMapper(deprecationResolver, local, configuration.unitSuspendFunctionExport)
            val namerConfiguration = createNamerConfiguration(configuration)
            val namer = ObjCExportNamerImpl(namerConfiguration, builtIns, mapper, local)

            return ObjcExportHeaderGeneratorMobile(
                    moduleDescriptors,
                    moduleTranslationConfig,
                    mapper,
                    namer,
                    problemCollector,
                    configuration.objcGenerics,
                    restrictToLocalModules,
                    sharedState
            )
        }
    }

    override fun shouldTranslateExtraClass(descriptor: ClassDescriptor): Boolean =
        !restrictToLocalModules || descriptor.module in moduleDescriptors
}
