package plugin

import java.io.File
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar.ExtensionRegistrarContext
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class Registrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(FirRegistrar())
        IrGenerationExtension.registerExtension(IrExtension())
    }

    override val supportsK2: Boolean
        get() = true
}

class FirRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        File("plugin-generated-file").writeText("frontend plugin applied")
    }
}

class IrExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        error("Backend plugins should not be applied in K2 kapt")
    }

    // Even though this returns true, this extension should not be applied in K2 as per KT-70333.
    @FirIncompatiblePluginAPI
    override val shouldAlsoBeAppliedInKaptStubGenerationMode: Boolean
        get() = true
}
