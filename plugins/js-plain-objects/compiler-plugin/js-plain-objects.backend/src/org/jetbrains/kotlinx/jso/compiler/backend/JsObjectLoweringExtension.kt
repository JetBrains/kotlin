/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlinx.jspo.compiler.backend

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.js.common.isES5IdentifierPart
import org.jetbrains.kotlin.js.common.isES5IdentifierStart
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.jspo.compiler.resolve.JsPlainObjectsPluginKey
import org.jetbrains.kotlinx.jspo.compiler.resolve.StandardIds
import kotlin.math.abs

private class MoveExternalInlineFunctionsWithBodiesOutsideLowering(private val context: IrPluginContext) : DeclarationTransformer {
    private val jsFunction = context.referenceFunctions(StandardIds.JS_FUNCTION_ID).single()
    private val EXPECTED_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(JsPlainObjectsPluginKey)

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        val file = declaration.file
        val parent = declaration.parentClassOrNull

        if (parent == null || declaration !is IrSimpleFunction || declaration.origin != EXPECTED_ORIGIN) return null

        val proxyFunction = declaration.createFunctionContainingTheLogic(parent).also(file::addChild)
        declaration.body = declaration.generateBodyWithTheProxyFunctionCall(proxyFunction)

        return null
    }

    private fun IrSimpleFunction.createFunctionContainingTheLogic(parent: IrClass): IrSimpleFunction {
        val declaration = this

        return context.irFactory.buildFun {
            updateFrom(declaration)
            name = declaration.name
            returnType = declaration.returnType
            origin = declaration.origin
            isInline = true
            isExternal = false
        }.apply {
            copyTypeParametersFrom(declaration)

            val substitutionMap = HashMap<IrTypeParameterSymbol, IrType>()
            substitutionMap.putAll(makeTypeParameterSubstitutionMap(declaration, this))

            if (!parent.isCompanion) {
                copyTypeParametersFrom(parent)
                substitutionMap.putAll(makeTypeParameterSubstitutionMap(parent, this))
            }

            copyValueParametersFrom(declaration, substitutionMap)

            extensionReceiverParameter = dispatchReceiverParameter
            dispatchReceiverParameter = null
            returnType = returnType.substitute(substitutionMap)

            body = when (declaration.name) {
                StandardNames.DATA_CLASS_COPY -> generateBodyForCopyFunction()
                OperatorNameConventions.INVOKE -> generateBodyForFactoryFunction()
                else -> error("Unexpected function with name `${declaration.name.identifier}`")
            }
        }
    }

    private fun IrSimpleFunction.generateBodyWithTheProxyFunctionCall(proxyFunction: IrSimpleFunction): IrBlockBody {
        val declaration = this
        return context.irFactory.createBlockBody(startOffset, declaration.endOffset).apply {
            statements += IrReturnImpl(
                declaration.startOffset,
                declaration.endOffset,
                declaration.returnType,
                declaration.symbol,
                IrCallImpl(
                    declaration.startOffset,
                    declaration.endOffset,
                    declaration.returnType,
                    proxyFunction.symbol,
                    proxyFunction.typeParameters.size,
                    proxyFunction.valueParameters.size,
                ).apply {
                    declaration.dispatchReceiverParameter?.let {
                        extensionReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.symbol)
                    }

                    for ((index, parameter) in declaration.valueParameters.withIndex()) {
                        putValueArgument(index, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.symbol))
                    }
                    for ((index, type) in declaration.typeParameters.withIndex()) {
                        putTypeArgument(index, type.defaultType)
                    }
                }
            )
        }
    }

    private fun IrSimpleFunction.generateBodyForFactoryFunction(): IrBlockBody {
        val declaration = this
        return context.irFactory.createBlockBody(startOffset, declaration.endOffset).apply {
            statements += IrReturnImpl(
                declaration.startOffset,
                declaration.endOffset,
                declaration.returnType,
                declaration.symbol,
                IrCallImpl(
                    declaration.startOffset,
                    declaration.endOffset,
                    declaration.returnType,
                    jsFunction,
                    0,
                    1,
                ).apply {
                    putValueArgument(0, createValueParametersObject(declaration.valueParameters).toIrConst(context.irBuiltIns.stringType))
                }
            )
        }
    }

    private fun sanitizeName(name: String): String {
        if (name.isEmpty()) return "_"

        val builder = StringBuilder(name.length + 7)

        val first = name.first()

        builder.append(first.mangleIfNot(Char::isES5IdentifierStart))

        for (idx in 1..name.lastIndex) {
            val c = name[idx]
            builder.append(c.mangleIfNot(Char::isES5IdentifierPart))
        }

        return "${builder}_${abs(name.hashCode()).toString(Character.MAX_RADIX)}"
    }

    private inline fun Char.mangleIfNot(predicate: Char.() -> Boolean) =
        if (predicate()) this else '_'

    private fun IrSimpleFunction.generateBodyForCopyFunction(): IrBlockBody {
        val declaration = this
        return context.irFactory.createBlockBody(startOffset, declaration.endOffset).apply {
            val selfName = Name.identifier("${"$$"}tmp_self${"$$"}")
            statements += IrVariableImpl(
                declaration.startOffset,
                declaration.endOffset,
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                IrVariableSymbolImpl(),
                selfName,
                context.irBuiltIns.nothingType,
                isVar = false,
                isConst = false,
                isLateinit = false
            ).apply {
                parent = declaration
                initializer = IrGetValueImpl(
                    declaration.startOffset,
                    declaration.endOffset,
                    declaration.extensionReceiverParameter!!.symbol
                )
            }
            statements += IrReturnImpl(
                declaration.startOffset,
                declaration.endOffset,
                declaration.returnType,
                declaration.symbol,
                IrCallImpl(
                    declaration.startOffset,
                    declaration.endOffset,
                    declaration.returnType,
                    jsFunction,
                    0,
                    1,
                ).apply {
                    val objectAssignCall = "Object.assign({}, ${selfName.identifier}, ${createValueParametersObject(declaration.valueParameters)})"
                    putValueArgument(0, objectAssignCall.toIrConst(context.irBuiltIns.stringType))
                }
            )
        }
    }

    private fun createValueParametersObject(valueParameters: Iterable<IrValueParameter>): String {
        val listOfParameters = valueParameters.joinToString(", ") {
            val (key, value) = it.name.run {
                if (identifier.isValidES5Identifier() && identifier !in RESERVED_KEYWORDS) identifier to identifier
                else {
                    val newName: Name = Name.identifier(sanitizeName(identifier)).apply { it.name = this }
                    "\"${identifier}\"" to newName.identifier
                }
            }
            "$key:$value"
        }

        return "{$listOfParameters}"
    }
}

open class JsPlainObjectsLoweringExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        MoveExternalInlineFunctionsWithBodiesOutsideLowering(pluginContext).lower(moduleFragment)
    }
}
