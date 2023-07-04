/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

import io.outfoxx.swiftpoet.CodeBlock
import io.outfoxx.swiftpoet.FileSpec
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.Modifier
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Generate a Swift API file for the given Kotlin IR module.
 *
 * A temporary solution to kick-start the work on Swift Export.
 * A proper solution is likely to be FIR-based and will be added later
 * as it requires a bit more work.
 *
 */
class IrBasedSwiftGenerator(private val moduleName: String) : IrElementVisitorVoid {

    private val initRuntimeIfNeededSpec = FunctionSpec.abstractBuilder("initRuntimeIfNeeded")
            .addAttribute("_silgen_name", "\"Kotlin_initRuntimeIfNeeded\"")
            .addModifiers(Modifier.FILEPRIVATE)
            .build()

    val functions = mutableListOf<FunctionSpec>(initRuntimeIfNeededSpec)

    fun build(): FileSpec =
        FileSpec.builder(moduleName, moduleName).apply {
            functions.forEach { topLevelFunction ->
                addFunction(topLevelFunction)
            }
        }.build()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (!isSupported(declaration)) {
            return
        }
        val name = declaration.name.identifier
        val symbolName = with(KonanBinaryInterface) { declaration.symbolName }
        val silgenFunctionSpec = FunctionSpec.abstractBuilder("${name}_bridge")
                .addAttribute("_silgen_name", "\"${symbolName}\"")
                .addModifiers(Modifier.FILEPRIVATE)
                .build()
        functions += silgenFunctionSpec
        val swiftFunctionSpec = FunctionSpec.builder(name)
                .addCode(CodeBlock.builder().addStatement("${initRuntimeIfNeededSpec.name}()").build())
                .addCode(CodeBlock.builder().addStatement("${silgenFunctionSpec.name}()").build())
                .build()
        functions += swiftFunctionSpec
    }

    private fun isSupported(declaration: IrFunction): Boolean {
        // For now, we support only () -> Unit functions.
        if (!declaration.returnType.isUnit()) {
            return false
        }
        if (declaration.allParameters.isNotEmpty()) {
            return false
        }
        return true
    }
}