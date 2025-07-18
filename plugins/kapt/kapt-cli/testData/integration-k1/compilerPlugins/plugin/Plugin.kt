package plugin

import java.io.File
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

class Registrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        TypeResolutionInterceptor.registerExtension(FrontendExtension())
        IrGenerationExtension.registerExtension(IrExtension1())
        IrGenerationExtension.registerExtension(IrExtension2())
    }

    override val supportsK2: Boolean
        get() = false
}

class FrontendExtension : TypeResolutionInterceptorExtension {
    override fun interceptType(element: KtElement, context: ExpressionTypingContext, resultType: KotlinType): KotlinType {
        File("plugin-generated-file").writeText("frontend plugin applied")
        return resultType
    }
}

class IrExtension1 : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        File("plugin-generated-file").appendText("\nbackend plugin applied")
    }

    @FirIncompatiblePluginAPI
    override val shouldAlsoBeAppliedInKaptStubGenerationMode: Boolean
        get() = true
}

class IrExtension2 : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        error("Backend plugin should not be applied in K1 kapt if shouldAlsoBeAppliedInKaptStubGenerationMode returns false")
    }

    @FirIncompatiblePluginAPI
    override val shouldAlsoBeAppliedInKaptStubGenerationMode: Boolean
        get() = false
}
