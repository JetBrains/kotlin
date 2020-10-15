/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializableCompanionIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializableIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializerIrGenerator

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

typealias SerializationPluginContext = IrPluginContext

private class SerializerClassLowering(
    val context: SerializationPluginContext,
    val metadataPlugin: SerializationDescriptorSerializerPlugin?
) :
    IrElementTransformerVoid(), ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        SerializableIrGenerator.generate(irClass, context, context.bindingContext)
        SerializerIrGenerator.generate(irClass, context, context.bindingContext, metadataPlugin)
        SerializableCompanionIrGenerator.generate(irClass, context, context.bindingContext)
    }
}

open class SerializationLoweringExtension @JvmOverloads constructor(val metadataPlugin: SerializationDescriptorSerializerPlugin? = null) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val serializerClassLowering = SerializerClassLowering(pluginContext, metadataPlugin)
        for (file in moduleFragment.files)
            serializerClassLowering.runOnFileInOrder(file)
    }
}