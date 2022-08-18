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
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerialInfoImplJvmIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializableCompanionIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializableIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializerIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.resolve.KSerializerDescriptorResolver
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
}

private class SerializerClassLowering(
    baseContext: IrPluginContext,
    metadataPlugin: SerializationDescriptorSerializerPlugin?,
    moduleFragment: IrModuleFragment
) : IrElementTransformerVoid(), ClassLoweringPass {
    val context: SerializationPluginContext = SerializationPluginContext(baseContext, metadataPlugin)
    private val serialInfoJvmGenerator = SerialInfoImplJvmIrGenerator(context, moduleFragment).also { context.serialInfoImplJvmIrGenerator = it }

    override fun lower(irClass: IrClass) {
        try {
            SerializableIrGenerator.generate(irClass, context)
            SerializerIrGenerator.generate(irClass, context, context.metadataPlugin, serialInfoJvmGenerator)
            SerializableCompanionIrGenerator.generate(irClass, context)

            if (context.platform.isJvm() && KSerializerDescriptorResolver.isSerialInfoImpl(irClass.descriptor)) {
                serialInfoJvmGenerator.generate(irClass)
            }
        } catch (e: Exception) {
            throw CompilationException("kotlinx.serialization compiler plugin internal error: unable to transform declaration, see cause", irClass.fileParent, irClass, e)
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
        val serializerClassLowering = SerializerClassLowering(pluginContext, metadataPlugin, moduleFragment)
        for (file in moduleFragment.files)
            serializerClassLowering.runOnFileInOrder(file)
    }
}
