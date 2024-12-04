/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.compiler.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class MyMethodGenerator : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        moduleFragment.accept(MyMethodTransformer(pluginContext), null)
    }

}

private class MyMethodTransformer(
    private val context: IrPluginContext,
) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)

        if (shouldGenerateMyMethod(declaration)) {
            generateMyMethod(declaration)
        }
    }

    private fun generateMyMethod(klass: IrClass) {
        val shouldGenerateBody = shouldGenerateMyMethodBody(klass)
        klass.addFunction(
            name = "myMethod",
            returnType = context.irBuiltIns.stringType,
            modality = if (shouldGenerateBody) Modality.OPEN else Modality.ABSTRACT,
        ).also { myMethod ->
            if (shouldGenerateBody) {
                myMethod.body = context.irFactory.createExpressionBody(
                    myMethod.startOffset, myMethod.endOffset,
                    IrConstImpl.string(
                        myMethod.startOffset,
                        myMethod.endOffset,
                        myMethod.returnType,
                        "Hello, world!"
                    )
                )
            }
        }
    }

    private fun shouldGenerateMyMethod(declaration: IrClass): Boolean =
        declaration.kind == ClassKind.CLASS || declaration.kind == ClassKind.INTERFACE

    private fun shouldGenerateMyMethodBody(declaration: IrClass): Boolean =
        declaration.kind == ClassKind.CLASS
}