/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.extensions

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm.JvmAtomicSymbols
import org.jetbrains.kotlinx.atomicfu.compiler.backend.js.AtomicfuJsIrTransformer
import org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm.AtomicfuJvmIrTransformer
import org.jetbrains.kotlinx.atomicfu.compiler.backend.native.AtomicfuNativeIrTransformer
import org.jetbrains.kotlinx.atomicfu.compiler.backend.native.NativeAtomicSymbols

public open class AtomicfuLoweringExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val platform = pluginContext.platform
        when {
            platform.isJvm() -> {
                val atomicSymbols = JvmAtomicSymbols(pluginContext, moduleFragment)
                AtomicfuJvmIrTransformer(pluginContext, atomicSymbols).transform(moduleFragment)
            }
            platform.isNative() -> {
                val atomicSymbols = NativeAtomicSymbols(pluginContext, moduleFragment)
                AtomicfuNativeIrTransformer(pluginContext, atomicSymbols).transform(moduleFragment)
            }
            platform.isJs() -> {
                for (file in moduleFragment.files) {
                    AtomicfuClassLowering(pluginContext).runOnFileInOrder(file)
                }
            }
        }
    }
}

private class AtomicfuClassLowering(
    val context: IrPluginContext
) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (context.platform.isJs()) {
            AtomicfuJsIrTransformer(context).transform(irFile)
        }
    }
}

/**
 * Copy of [runOnFilePostfix], but this implementation first lowers declaration, then its children.
 */
fun FileLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFile(declaration: IrFile) {
            lower(declaration)
            declaration.acceptChildrenVoid(this)
        }
    })
}
