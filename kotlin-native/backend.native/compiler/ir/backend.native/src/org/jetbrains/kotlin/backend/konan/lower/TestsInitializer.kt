/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.NativeLoweringContext
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.sourcesModules
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.SetDeclarationsParentVisitor
import org.jetbrains.kotlin.ir.util.hasAnnotation
import kotlin.collections.plus

/**
 * Returns `true` if `@Test` from [irFile] need to be registered.
 *
 * Only the tests of the modules compiled from sources (including the `-Xinclude`d libraries) are to be registered.
 * Usually, the test initializers of the other libraries don't even get here: being private and unreferenced they aren't
 * deserialized. But when building with caches a library is deserialized in full, so its test initializers
 * must be skipped explicitly, otherwise its tests would leak to the test binaries depending on that library.
 */
internal fun NativeLoweringContext.shouldProcessFileWithTests(irFile: IrFile): Boolean {
    val sourceModules = configuration.sourcesModules
            ?: error("sourcesModules must have been set in the configuration by the frontend phase")
    return irFile.moduleDescriptor in sourceModules
}

internal class TestsInitializer(private val context: NativeLoweringContext) : FileLoweringPass {
    private val symbols = context.symbols

    override fun lower(irFile: IrFile) {
        if (!context.shouldProcessFileWithTests(irFile)) return

        for (idx in irFile.declarations.indices) {
            val function = irFile.declarations[idx] as? IrSimpleFunction ?: continue
            if (!function.hasAnnotation(symbols.testInitializer)) continue
            irFile.declarations[idx] = context.irFactory.buildField {
                startOffset = function.startOffset
                endOffset = function.endOffset
                name = "createTestSuites".synthesizedName
                visibility = DescriptorVisibilities.PRIVATE
                isFinal = true
                isStatic = true
                type = context.irBuiltIns.unitType
            }.apply {
                parent = irFile
                annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, symbols.eagerInitialization.owner)
                annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, symbols.threadLocal.owner)
                val statements = (function.body as IrBlockBody).statements
                statements.forEach { it.accept(SetDeclarationsParentVisitor, this) }
                initializer = context.irFactory.createExpressionBody(
                        startOffset, endOffset,
                        IrCompositeImpl(startOffset, endOffset, context.irBuiltIns.unitType, null, statements)
                )
            }
        }
    }
}
