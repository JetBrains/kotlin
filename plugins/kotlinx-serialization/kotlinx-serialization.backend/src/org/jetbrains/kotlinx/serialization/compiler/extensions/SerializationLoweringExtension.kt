/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrIntrinsicExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.*
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializationJvmIrIntrinsicSupport
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages
import java.util.concurrent.ConcurrentHashMap

/**
 * Copy of [runOnFilePostfix], but this implementation first lowers declaration, then its children.
 */
fun ClassLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            lower(declaration)
            declaration.acceptChildrenVoid(this)
        }
    })
}


class SerializationPluginContext(baseContext: IrPluginContext, val metadataPlugin: SerializationDescriptorSerializerPlugin?) :
    IrPluginContext by baseContext, SerializationBaseContext {

    internal val copiedStaticWriteSelf: MutableMap<IrSimpleFunction, IrSimpleFunction> = ConcurrentHashMap()

    internal val enumSerializerFactoryFunc = baseContext.referenceFunctions(
        CallableId(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME
        )
    ).singleOrNull()

    internal val markedEnumSerializerFactoryFunc = baseContext.referenceFunctions(
        CallableId(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.MARKED_ENUM_SERIALIZER_FACTORY_FUNC_NAME
        )
    ).singleOrNull()

    override val runtimeHasEnumSerializerFactoryFunctions = enumSerializerFactoryFunc != null && markedEnumSerializerFactoryFunc != null

    override fun referenceClassId(classId: ClassId): IrClassSymbol? = referenceClass(classId)
}

private inline fun IrClass.runPluginSafe(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        throw CompilationException(
            "kotlinx.serialization compiler plugin internal error: unable to transform declaration, see cause",
            this.fileParent,
            this,
            e
        )
    }
}

private class SerializerClassLowering(
    baseContext: IrPluginContext,
    metadataPlugin: SerializationDescriptorSerializerPlugin?,
    moduleFragment: IrModuleFragment
) : IrElementTransformerVoid(), ClassLoweringPass {
    val context: SerializationPluginContext = SerializationPluginContext(baseContext, metadataPlugin)

    // Lazy to avoid creating generator in non-JVM backends
    private val serialInfoJvmGenerator by lazy(LazyThreadSafetyMode.NONE) { SerialInfoImplJvmIrGenerator(context, moduleFragment) }

    override fun lower(irClass: IrClass) {
        irClass.runPluginSafe {
            SerializableIrGenerator.generate(irClass, context)
            SerializerIrGenerator.generate(irClass, context, context.metadataPlugin)
            SerializableCompanionIrGenerator.generate(irClass, context)

            if (context.platform.isJvm() && irClass.isSerialInfoAnnotation) {
                serialInfoJvmGenerator.generateImplementationFor(irClass)
            }
        }
    }
}

private class SerializerClassPreLowering(
    baseContext: IrPluginContext
) : IrElementTransformerVoid(), ClassLoweringPass {
    val context: SerializationPluginContext = SerializationPluginContext(baseContext, null)

    override fun lower(irClass: IrClass) {
        irClass.runPluginSafe {
            IrPreGenerator.generate(irClass, context)
        }
    }
}

enum class SerializationIntrinsicsState {
    NORMAL, // depends on whether we have noCompiledSerializer function in runtime
    DISABLED, // disabled if corresponding CLI flag passed
    FORCE_ENABLED // used for test purposes ONLY
}

open class SerializationLoweringExtension @JvmOverloads constructor(
    private val metadataPlugin: SerializationDescriptorSerializerPlugin? = null
) : IrGenerationExtension {

    private var intrinsicsState = SerializationIntrinsicsState.NORMAL

    constructor(metadataPlugin: SerializationDescriptorSerializerPlugin, intrinsicsState: SerializationIntrinsicsState) : this(metadataPlugin) {
        this.intrinsicsState = intrinsicsState
    }

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val pass1 = SerializerClassPreLowering(pluginContext)
        val pass2 = SerializerClassLowering(pluginContext, metadataPlugin, moduleFragment)
        moduleFragment.files.forEach(pass1::runOnFileInOrder)
        moduleFragment.files.forEach(pass2::runOnFileInOrder)
    }

    override fun getPlatformIntrinsicExtension(backendContext: BackendContext): IrIntrinsicExtension? {
        val ctx = backendContext as? JvmBackendContext ?: return null
        if (!canEnableIntrinsics(ctx)) return null
        return SerializationJvmIrIntrinsicSupport(ctx)
    }

    private fun canEnableIntrinsics(ctx: JvmBackendContext): Boolean {
        return when (intrinsicsState) {
            SerializationIntrinsicsState.FORCE_ENABLED -> true
            SerializationIntrinsicsState.DISABLED -> false
            SerializationIntrinsicsState.NORMAL -> {
                val module = ctx.state.module
                if (module.findClassAcrossModuleDependencies(
                        ClassId(
                            SerializationPackages.packageFqName,
                            SerialEntityNames.KSERIALIZER_NAME
                        )
                    ) == null
                ) return false
                module.getPackage(SerializationPackages.packageFqName).memberScope.getFunctionNames()
                    .any { it.asString() == SerializationJvmIrIntrinsicSupport.noCompiledSerializerMethodName }
            }
        }
    }
}


