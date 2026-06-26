package repro.plugin

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirFunctionTypeKindExtension
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private val pluginFunctionAnnotationId = ClassId.topLevel(FqName("repro.PluginFunction"))

private val useLegacyCustomFunctionTypeSerializationUntil: String
    get() {
        return LanguageVersion.values().last().versionString
    }

@OptIn(ExperimentalCompilerApi::class)
class FunctionKindPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = "repro.function-kind"

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(FunctionKindExtensionRegistrar())
    }
}

private class FunctionKindExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::FunctionKindExtension
    }
}

private class FunctionKindExtension(session: FirSession) : FirFunctionTypeKindExtension(session) {
    override fun FunctionTypeKindRegistrar.registerKinds() {
        registerKind(PluginFunctionKind, KPluginFunctionKind)
    }
}

private object PluginFunctionKind : FunctionTypeKind(
    FqName("repro.internal"),
    "PluginFunction",
    pluginFunctionAnnotationId,
    isReflectType = false,
    isInlineable = true,
) {
    override val prefixForTypeRender: String
        get() = "@PluginFunction"

    override val serializeAsFunctionWithAnnotationUntil: String
        get() = useLegacyCustomFunctionTypeSerializationUntil

    override fun reflectKind(): FunctionTypeKind = KPluginFunctionKind
}

private object KPluginFunctionKind : FunctionTypeKind(
    FqName("repro.internal"),
    "KPluginFunction",
    pluginFunctionAnnotationId,
    isReflectType = true,
    isInlineable = false,
) {
    override val prefixForTypeRender: String
        get() = "@PluginFunction"

    override val serializeAsFunctionWithAnnotationUntil: String
        get() = useLegacyCustomFunctionTypeSerializationUntil

    override fun nonReflectKind(): FunctionTypeKind = PluginFunctionKind
}
