package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.sx.SXClangModuleBuilder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class ObjcExportHeaderGeneratorMobile internal constructor(
        moduleDescriptors: List<ModuleDescriptor>,
        mapper: ObjCExportMapper,
        private val restrictToLocalModules: Boolean,
        eventQueue: EventQueue,
) : ObjCExportModulesIndexer(
        moduleDescriptors,
        mapper,
        eventQueue,
) {

    companion object {
        fun createInstance(
                configuration: ObjCExportLazy.Configuration,
                moduleDescriptors: List<ModuleDescriptor>,
                deprecationResolver: DeprecationResolver? = null,
                local: Boolean = false,
                restrictToLocalModules: Boolean = false,
                eventQueue: EventQueue
        ): ObjCExportModulesIndexer {
            val mapper = ObjCExportMapper(deprecationResolver, local, configuration.unitSuspendFunctionExport)
            return ObjcExportHeaderGeneratorMobile(
                    moduleDescriptors,
                    mapper,
                    restrictToLocalModules,
                    eventQueue
            )
        }
    }

    override fun shouldTranslateExtraClass(descriptor: ClassDescriptor): Boolean =
            !restrictToLocalModules || descriptor.module in moduleDescriptors
}
