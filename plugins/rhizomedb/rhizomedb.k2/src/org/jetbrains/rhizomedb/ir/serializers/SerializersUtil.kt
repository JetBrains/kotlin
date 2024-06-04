/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.ir.serializers

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName

data class CompilerPluginContext(
    val moduleFragment: IrModuleFragment,
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
)

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrBuilderWithScope.generateSerializerCall(
    irType: IrType,
    context: CompilerPluginContext,
    debugInfo: String,
    isTypeArgument: Boolean = false,
): IrExpression {
    val irClass = irType.classOrFail.owner

    // fallback to the class itself, for example Unit serializer() is defined on Unit since it has no Companion
    val companionOrClass = irClass.companionObject() ?: irClass

    val serializerCall =
        // check if the class is one of the special ones that don't have serializer on Companion (List/Set/etc)
        context.getSpecialSerializer(irClass.kotlinFqName)?.let { irCall(it) }
            ?: findSerializerAsMethod(companionOrClass)
            ?: findSerializerAsExtensionFunction(companionOrClass, context)

    val serializer = serializerCall?.apply {
        for ((index, argument) in (irType as IrSimpleType).arguments.withIndex()) {
            putValueArgument(index, generateSerializerCall(argument.typeOrFail, context, debugInfo, isTypeArgument = true))
        }
    }

    return if (serializer == null) {
        val message = "Couldn't find serializer function on companion object for ${irClass.dumpKotlinLike()}\nAdditional info:\n$debugInfo"
        if (isTypeArgument) {
            context.messageCollector.report(
                CompilerMessageSeverity.WARNING,
                message
            )
            irNull() // if RemoteKind.Data.serializer == null, we will use ThrowingSerializer
        } else {
            error(message)
        }
    } else if (irType.isNullable()) {
        irCall(context.nullableSerializerProperty.getter!!).apply {
            extensionReceiver = serializer
        }
    } else {
        serializer
    }
}

// find .serializer() as an extension function
@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrBuilderWithScope.findSerializerAsExtensionFunction(
    companionOrClass: IrClass,
    context: CompilerPluginContext,
): IrFunctionAccessExpression? {
    val classId = checkNotNull(companionOrClass.classId) { "No classId for ${companionOrClass.dumpKotlinLike()}" }

    val serializerFunctions =
        context.pluginContext.referenceFunctions(
            CallableId(
                packageName = classId.packageFqName,
                className = null,
                callableName = "serializer".name
            )
        ) + context.pluginContext.referenceFunctions(
            CallableId(
                packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
                className = null,
                callableName = "serializer".name
            )
        )

    val serializerFunction =
        serializerFunctions.find { it.owner.extensionReceiverParameter?.type == companionOrClass.defaultType }

    return serializerFunction?.let {
        irCall(serializerFunction).apply {
            extensionReceiver = irGetObject(companionOrClass.symbol)
        }
    }
}

// find .serializer() as a method of irClass
@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrBuilderWithScope.findSerializerAsMethod(irClass: IrClass): IrFunctionAccessExpression? {
    return irClass.functions.find { it.name.identifierOrNullIfSpecial == "serializer" }?.let {
        irCall(it).apply {
            dispatchReceiver = irGetObject(irClass.symbol)
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private val CompilerPluginContext.nullableSerializerProperty
    get() = remember {
        pluginContext.referenceProperties(
            CallableId(
                packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
                className = null,
                callableName = "nullable".name
            )
        ).first().owner
    }
