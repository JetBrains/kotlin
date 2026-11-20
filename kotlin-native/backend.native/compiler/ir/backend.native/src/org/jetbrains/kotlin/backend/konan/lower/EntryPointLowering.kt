/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addFile
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import kotlin.collections.plus

internal val DECLARATION_ORIGIN_ENTRY_POINT = IrDeclarationOriginImpl("ENTRY_POINT")

/**
 * Creates an entry point named `Konan_start` that wraps the user-defined entry point.
 *
 * * `Konan_start` is exposed to the K/N runtime under the same C name
 * * the signature is always `(Array<String>) -> Int`; when the user-defined entry point is parameterless, this wrapper just ignores
 *   its argument
 * * when the user-defined entry point throws an exception, it uses `processUnhandledException` and `terminateWithUnhandledException`
 */
internal class EntryPointLowering(val context: NativeGenerationState) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val actualMain = context.symbols.entryPoint!!.owner
        val actualMainInCurrentCompilationUnit = context.llvmModuleSpecification.containsDeclaration(actualMain)

        val entryPoint = makeEntryPoint(actualMain, actualMainInCurrentCompilationUnit)

        val file: IrFile = if (actualMainInCurrentCompilationUnit) {
            actualMain.file
        } else {
            // `main` function is compiled to other LLVM module.
            // For example, test running support uses `main` defined in stdlib.
            irModule.addFile(NaiveSourceBasedFileEntryImpl("entryPointOwner"), FqName("kotlin.native.internal.abi"))
        }

        file.addChild(entryPoint)
    }

    private fun IrModuleFragment.addFile(fileEntry: IrFileEntry, packageFqName: FqName): IrFile {
        val packageFragmentDescriptor = object : PackageFragmentDescriptorImpl(this.descriptor, packageFqName) {
            override fun getMemberScope(): MemberScope = MemberScope.Empty
        }

        return IrFileImpl(fileEntry, packageFragmentDescriptor, this).also(this::addFile)
    }

    private fun makeEntryPoint(actualMain: IrSimpleFunction, actualMainInCurrentCompilationUnit: Boolean): IrFunction {
        // TODO: Do we need to do something with the offsets if <main> is in a cached library?
        val startOffset = if (actualMainInCurrentCompilationUnit)
            actualMain.startOffset
        else
            SYNTHETIC_OFFSET
        val endOffset = if (actualMainInCurrentCompilationUnit)
            actualMain.endOffset
        else
            SYNTHETIC_OFFSET
        val entryPoint = context.irFactory.buildFun {
            this.startOffset = startOffset
            this.endOffset = endOffset
            origin = DECLARATION_ORIGIN_ENTRY_POINT
            name = Name.identifier("Konan_start")
            visibility = DescriptorVisibilities.PRIVATE
            returnType = context.irBuiltIns.intType
        }.apply {
            addValueParameter {
                this.startOffset = startOffset
                this.endOffset = endOffset
                origin = DECLARATION_ORIGIN_ENTRY_POINT
                name = Name.identifier("args")
                type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)
            }
        }
        entryPoint.annotations += buildSimpleAnnotation(context.irBuiltIns,
                startOffset, endOffset,
                context.symbols.exportForCppRuntime.owner, "Konan_start")

        val builder = context.createIrBuilder(entryPoint.symbol, startOffset, endOffset)
        entryPoint.body = builder.irBlockBody(entryPoint) {
            +IrTryImpl(startOffset, endOffset, context.irBuiltIns.nothingType).apply {
                tryResult = irBlock {
                    +irCall(actualMain).apply {
                        when (actualMain.parameters.size) {
                            0 -> Unit
                            1 -> arguments[0] = irGet(entryPoint.parameters[0])
                            else -> error("Too many parameters")
                        }
                    }
                    +irReturn(irInt(0))
                }
                val catchParameter = buildVariable(
                        builder.parent, startOffset, endOffset,
                        IrDeclarationOrigin.CATCH_PARAMETER,
                        Name.identifier("e"),
                        context.irBuiltIns.throwableType
                )
                catches += irCatch(
                        catchParameter,
                        result = irBlock {
                            +irCall(this@EntryPointLowering.context.symbols.processUnhandledException).apply {
                                arguments[0] = irGet(catchParameter)
                            }
                            +irCall(this@EntryPointLowering.context.symbols.terminateWithUnhandledException).apply {
                                arguments[0] = irGet(catchParameter)
                            }
                        }
                )
            }
        }

        return entryPoint
    }
}