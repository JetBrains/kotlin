/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.specialization.ir

import org.jetbrains.kotlin.common.IrDefaultElementVisitor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.isStrictSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ClassInheritorsCollector(private val baseClass: IrClass): IrDefaultElementVisitor() {
    val inheritors = mutableListOf<IrClass>()

    override fun visitClass(declaration: IrClass) {
        if (declaration.symbol.isStrictSubtypeOfClass(baseClass.symbol)) {
            inheritors.add(declaration)
        }
    }
}

fun getAllInheritors(cls: IrClass, module: IrElement): List<IrClass> {
    val collector = ClassInheritorsCollector(cls)
    module.acceptVoid(collector)
    return collector.inheritors
}