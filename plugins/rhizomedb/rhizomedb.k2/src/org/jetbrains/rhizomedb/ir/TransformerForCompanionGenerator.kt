/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.rhizomedb.fir.RhizomedbPluginKey

class TransformerForCompanionGenerator(private val context: IrPluginContext) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        val origin = declaration.origin
        if (origin !is GeneratedByPlugin || origin.pluginKey != RhizomedbPluginKey) {
            visitElement(declaration)
            return
        }
        val getter = declaration.getter ?: declaration.addGetter()
//        require(getter.body == null)

        val req = context.irBuiltIns.createIrBuilder(declaration.symbol).run {
            irCall(context.irBuiltIns.requiredTransientFunction).apply {
                dispatchReceiver = irGetObject((declaration.parent as IrClass).symbol)
                putValueArgument(0, irString(declaration.name.asString()))
            }
        }

        val field = context.irFactory.buildField {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("${declaration.name.identifier}-field")
            type = getter.returnType
            visibility = DescriptorVisibilities.PRIVATE_TO_THIS
            isStatic = context.platform.isJvm()
        }.also { f ->
            f.correspondingPropertySymbol = declaration.symbol
            f.parent = declaration.parent
            f.initializer = context.irFactory.createExpressionBody(-1, -1, req)
        }
        declaration.backingField = field
    }
}
