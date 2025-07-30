/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaCompanionKey
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class IrImportedSchemaGenerator(
    val context: IrPluginContext,
) : FileLoweringPass,
    IrElementTransformerVoid() {

    companion object {
        val io = FqName("org.jetbrains.kotlinx.dataframe.io")
        val schemaReaderId = ClassId(io, Name.identifier("SchemaReader"))
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val origin = declaration.origin
        if (origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey is ImportedSchemaCompanionKey) {
            val metadata = (origin.pluginKey as ImportedSchemaCompanionKey).metadata
            val param = when (declaration.name) {
                Names.DEFAULT -> metadata.data.toIrConst(context.irBuiltIns.stringType)
                Names.READ -> IrGetValueImpl(-1, -1, declaration.parameters[1].symbol)
                else -> return declaration
            }

            val providerFun = CallableId(schemaReaderId, declaration.name)
            val provide = context.referenceFunctions(providerFun).firstOrNull()
                ?: error("Couldn't find a function $providerFun")

            val actualSchemaReaderClassId =
                ClassId(FqName(metadata.format.substringBeforeLast(".")), Name.identifier(metadata.format.substringAfterLast(".")))

            val zeroArg = context.referenceConstructors(actualSchemaReaderClassId).firstOrNull {
                // empty or all default
                it.owner.parameters.all { it.defaultValue != null }
            }
                ?: error("Couldn't find zero argument constructor for class $actualSchemaReaderClassId")

            context.irBuiltIns.createIrBuilder(declaration.symbol).apply {
                declaration.body = irBlockBody {
                    val schemaReaderInstance = irCallConstructor(zeroArg, emptyList())
                    val call = irCall(provide).also {
                        it.arguments[0] = schemaReaderInstance
                        it.arguments[1] = param
                    }
                    val typeOp = typeOperator(
                        resultType = call.type,
                        argument = call,
                        typeOperator = IrTypeOperator.CAST,
                        typeOperand = declaration.returnType
                    )

                    +irReturn(typeOp)
                }
            }
        }
        return super.visitFunction(declaration)
    }


    val typeOf = context.referenceFunctions(CallableId(FqName("kotlin.reflect"), Name.identifier("typeOf"))).single()

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val getter = declaration.getter ?: return declaration
        val schemaType = declaration.parentClassOrNull?.parentClassOrNull?.defaultType ?: return declaration
        if (declaration.origin.isFromDataSchemaSource() && declaration.name == Names.SCHEMA_KTYPE) {
            context.irBuiltIns.createIrBuilder(getter.symbol).apply {
                val call = irCall(typeOf).also {
                    it.typeArguments[0] = schemaType
                }
                getter.body = irBlockBody {
                    +irReturn(call)
                }
            }
        }
        return declaration
    }

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        if (declaration.origin.isFromDataSchemaSource()) {
            declaration.body = generateBodyForDefaultConstructor(context, declaration)
        }
        return declaration
    }

    private fun IrDeclarationOrigin.isFromDataSchemaSource(): Boolean =
        this is IrDeclarationOrigin.GeneratedByPlugin && this.pluginKey is ImportedSchemaCompanionKey
}