package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * This lowering pass replaces [IrStringConcatenation]s with StringBuilder appends.
 */
internal class StringConcatenationLowering(context: Context) : FileLoweringPass, IrBuildingTransformer(context) {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    private val irBuiltIns = context.irBuiltIns
    private val symbols = context.ir.symbols

    private val typesWithSpecialAppendFunction = irBuiltIns.primitiveIrTypes + irBuiltIns.stringType

    private val nameToString = Name.identifier("toString")
    private val nameAppend = Name.identifier("append")

    private val stringBuilder = context.ir.symbols.stringBuilder.owner

    //TODO: calculate and pass string length to the constructor.
    private val constructor = stringBuilder.constructors.single {
        it.valueParameters.isEmpty()
    }

    private val toStringFunction = stringBuilder.functions.single {
        it.valueParameters.isEmpty() && it.name == nameToString
    }

    private val defaultAppendFunction = stringBuilder.functions.single {
        it.name == nameAppend &&
                it.valueParameters.size == 1 &&
                it.valueParameters.single().type.isNullableAny()
    }

    private val appendFunctions: Map<IrType, IrSimpleFunction?> =
            typesWithSpecialAppendFunction.map { type ->
                type to stringBuilder.functions.toList().atMostOne {
                    it.name == nameAppend && it.valueParameters.singleOrNull()?.type == type
                }
            }.toMap()

    private fun typeToAppendFunction(type: IrType): IrSimpleFunction {
        return appendFunctions[type] ?: defaultAppendFunction
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression.transformChildrenVoid(this)

        builder.at(expression)
        val arguments = expression.arguments
        return when {
            arguments.isEmpty() -> builder.irString("")

            arguments.size == 1 -> {
                val argument = arguments[0]
                if (argument.type.isNullable())
                    builder.irCall(symbols.extensionToString).apply {
                        extensionReceiver = argument
                    }
                else builder.irCall(
                        irBuiltIns.anyClass.functions
                                .single { it.owner.name.asString() == "toString" }).apply {
                    dispatchReceiver = argument
                }
            }

            arguments.size == 2 && arguments[0].type.isStringClassType() ->
                if (arguments[0].type.isNullable())
                    builder.irCall(symbols.stringPlus).apply {
                        extensionReceiver = arguments[0]
                        putValueArgument(0, arguments[1])
                    }
                else
                    builder.irCall(symbols.string.functions
                            .single { it.owner.name == OperatorNameConventions.PLUS }).apply {
                        dispatchReceiver = arguments[0]
                        putValueArgument(0, arguments[1])
                    }

            else -> builder.irBlock(expression) {
                val stringBuilderImpl = createTmpVariable(irCall(constructor))
                expression.arguments.forEach { arg ->
                    val appendFunction = typeToAppendFunction(arg.type)
                    +irCall(appendFunction).apply {
                        dispatchReceiver = irGet(stringBuilderImpl)
                        putValueArgument(0, arg)
                    }
                }
                +irCall(toStringFunction).apply {
                    dispatchReceiver = irGet(stringBuilderImpl)
                }
            }
        }
    }
}
