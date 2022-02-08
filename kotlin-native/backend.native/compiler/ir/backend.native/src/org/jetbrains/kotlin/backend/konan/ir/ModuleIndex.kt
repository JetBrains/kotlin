/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ModuleIndex(val module: IrModuleFragment) {

    /**
     * Contains all classes declared in [module]
     */
    val classes: Map<ClassDescriptor, IrClass>

    /**
     * Contains all functions declared in [module]
     */
    val functions: Map<FunctionDescriptor, IrFunction>

    /**
     * Contains all properties declared in [module]
     */
    val properties: Map<PropertyDescriptor, IrProperty>

    init {
        classes = mutableMapOf()
        functions = mutableMapOf()
        properties = mutableMapOf()

        module.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                super.visitClass(declaration)

                classes[declaration.descriptor] = declaration
            }

            override fun visitFunction(declaration: IrFunction) {
                super.visitFunction(declaration)
                functions[declaration.descriptor] = declaration
            }

            override fun visitProperty(declaration: IrProperty) {
                super.visitProperty(declaration)
                properties[declaration.descriptor] = declaration
            }
        })
    }
}
