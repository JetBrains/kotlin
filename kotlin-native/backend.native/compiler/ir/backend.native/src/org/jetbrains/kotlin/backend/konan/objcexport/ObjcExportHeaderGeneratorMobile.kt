package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver

class ObjcExportHeaderGeneratorMobile internal constructor(
        moduleDescriptors: List<ModuleDescriptor>,
        mapper: ObjCExportMapper,
        namer: ObjCExportNamer,
        private val warningCollector: ObjCExportWarningCollector,
        objcGenerics: Boolean
) : ObjCExportHeaderGenerator(moduleDescriptors, mapper, namer, objcGenerics) {

    companion object {
        fun createInstance(
                configuration: ObjCExportLazy.Configuration,
                warningCollector: ObjCExportWarningCollector,
                builtIns: KotlinBuiltIns,
                moduleDescriptors: List<ModuleDescriptor>,
                deprecationResolver: DeprecationResolver? = null,
                local: Boolean = false): ObjCExportHeaderGenerator {

            val mapper = ObjCExportMapper(deprecationResolver, local)
            val namerConfiguration = createNamerConfiguration(configuration)
            val namer = ObjCExportNamerImpl(namerConfiguration, builtIns, mapper, local)

            return ObjcExportHeaderGeneratorMobile(
                    moduleDescriptors,
                    mapper,
                    namer,
                    warningCollector,
                    configuration.objcGenerics)
        }
    }

    override fun reportWarning(text: String) {
        warningCollector.reportWarning(text)
    }

    override fun reportWarning(method: FunctionDescriptor, text: String) {
        warningCollector.reportWarning(method, text)
    }

}
