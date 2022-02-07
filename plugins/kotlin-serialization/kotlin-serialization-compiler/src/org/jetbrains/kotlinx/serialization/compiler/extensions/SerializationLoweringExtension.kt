/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethod
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerialInfoImplJvmIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializableCompanionIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializableIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializerIrGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.JvmSerializerIntrinsic
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.JvmSerializerIntrinsic.isSerializerReifiedFunction
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.kSerializerType
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
        SerializableIrGenerator.generate(irClass, context, context.bindingContext)
        SerializerIrGenerator.generate(irClass, context, context.bindingContext, context.metadataPlugin, serialInfoJvmGenerator)
        SerializableCompanionIrGenerator.generate(irClass, context, context.bindingContext)

        if (context.platform.isJvm() && KSerializerDescriptorResolver.isSerialInfoImpl(irClass.descriptor)) {
            serialInfoJvmGenerator.generate(irClass)
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

    override fun retrieveIntrinsic(symbol: IrFunctionSymbol): Any? {
        val targetFunction = symbol.descriptor // todo: get rid of descriptor
        if (!isSerializerReifiedFunction(targetFunction)) return null
        return SerializationIrIntrinsic
    }
}

object SerializationIrIntrinsic : IntrinsicMethod() {
    override fun invoke(
        expression: IrFunctionAccessExpression,
        codegen: ExpressionCodegen,
        data: BlockInfo
    ): PromisedValue? {
        with(codegen) {
            val argument = expression.getTypeArgument(0)!!
            JvmSerializerIntrinsic.generateSerializerForType(
                argument.toKotlinType(),
                mv,
                codegen.context.state.typeMapper,
                codegen.context.state.typeMapper.typeSystem,
                state.module
            )
            return MaterialValue(codegen, kSerializerType, expression.type)
        }
    }
}
