/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.coverage.compiler.instrumentation

import org.jetbrains.kotlin.coverage.compiler.common.KotlinCoverageInstrumentationContext
import org.jetbrains.kotlin.coverage.compiler.hit.HitRegistrar
import org.jetbrains.kotlin.coverage.compiler.metadata.DeclarationContainerIM
import org.jetbrains.kotlin.coverage.compiler.metadata.FileIM
import org.jetbrains.kotlin.coverage.compiler.metadata.positionRange
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Class to walk over an IR declarations tree, add required declarations and instrument functions.
 */
internal class IrFileVisitor(
    val irFile: IrFile,
    val fileIM: FileIM,
    val instrumenter: Instrumenter,
    val hitRegistrar: HitRegistrar,
    val context: KotlinCoverageInstrumentationContext,
) {
    internal fun process() {
        // TODO can we use file to collect coverage or we should place only in classes/objects/companions/top level files

        // register declarations
        hitRegistrar.extraDeclarations.forEach { irDeclaration ->
            irDeclaration.parent = irFile
            irFile.declarations.add(irDeclaration)
        }

        fileIM.processTopLevelContainer(irFile)

        hitRegistrar.finalize()
    }

    private fun DeclarationContainerIM.processTopLevelContainer(container: IrDeclarationContainer) {
        container.declarations.forEach { irDeclaration ->
            if (irDeclaration.origin != IrDeclarationOrigin.DEFINED) return@forEach

            when (irDeclaration) {
                is IrProperty -> processProperty(irDeclaration)
                is IrClass -> processClass(irDeclaration)
                is IrFunction -> processFunction(irDeclaration)
            }
        }
    }

    private fun DeclarationContainerIM.processClass(irClass: IrClass) {
        // TODO is init{} block always injected to <init>?
        val classIM = addClass(irClass.name.asString(), irClass.isCompanion, irFile.positionRange(irClass))
        classIM.processTopLevelContainer(irClass)
    }

    private fun DeclarationContainerIM.processFunction(irFunction: IrFunction) {
        // TODO (irClass.parent as IrFunction).body?.statements?.filterIsInstance<IrClass>()

        val range = irFile.positionRange(irFunction)
        val functionIM = addFunction(irFunction.name.asString(), listOf(), "", range)

        instrumenter.instrument(irFunction, functionIM, irFile, hitRegistrar, context)
    }

    private fun DeclarationContainerIM.processProperty(irProperty: IrProperty) {
        if (irProperty.getter == null && irProperty.setter == null) return

        irProperty.backingField?.initializer

        val range = irFile.positionRange(irProperty)
        val propertyIM = addProperty(irProperty.name.asString(), irProperty.isVar, irProperty.isConst, range)

        // TODO irProperty.backingField?.initializer
    }

}