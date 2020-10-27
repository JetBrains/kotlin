package org.jetbrains.konan.test.plugin.nop

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

open class NopExtension : IrGenerationExtension {
    override fun generate(
            moduleFragment: IrModuleFragment,
            pluginContext: IrPluginContext
    ) {

    }
}