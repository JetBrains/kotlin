/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.KSerializerDescriptorResolver
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
    IrPluginContext by baseContext {
    lateinit var serialInfoImplJvmIrGenerator: SerialInfoImplJvmIrGenerator

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

    val runtimeHasEnumSerializerFactoryFunctions = enumSerializerFactoryFunc != null && markedEnumSerializerFactoryFunc != null
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
    private val serialInfoJvmGenerator =
        SerialInfoImplJvmIrGenerator(context, moduleFragment).also { context.serialInfoImplJvmIrGenerator = it }

    override fun lower(irClass: IrClass) {
        irClass.runPluginSafe {
            SerializableIrGenerator.generate(irClass, context)
            SerializerIrGenerator.generate(irClass, context, context.metadataPlugin)
            SerializableCompanionIrGenerator.generate(irClass, context)

            @OptIn(ObsoleteDescriptorBasedAPI::class)
            if (context.platform.isJvm() && KSerializerDescriptorResolver.isSerialInfoImpl(irClass.descriptor)) {
                serialInfoJvmGenerator.generate(irClass)
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

open class SerializationLoweringExtension @JvmOverloads constructor(
    val metadataPlugin: SerializationDescriptorSerializerPlugin? = null
) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val pass1 = SerializerClassPreLowering(pluginContext)
        val pass2 = SerializerClassLowering(pluginContext, metadataPlugin, moduleFragment)
        moduleFragment.files.forEach(pass1::runOnFileInOrder)
        moduleFragment.files.forEach(pass2::runOnFileInOrder)
    }
}
