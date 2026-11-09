/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.IrTypeVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Records the uses of the representation of value classes: a value class is passed, stored and returned unboxed,
 * so the code of a file mentioning one depends on its underlying type, even when it references no declaration
 * whose signature changes with it (e.g. `fun pass(v: V): V = v`). The representation of a value class is recorded
 * by its [Fact.BaseClassLayout], which hashes its fields.
 *
 * The underlying type may itself be a value class, possibly from yet another file: the uses are recorded transitively.
 */
internal object ValueClassRepresentationUses {

    /** Records the uses of the value classes mentioned anywhere in the (lowered) files of [module]. */
    fun recordFrom(module: IrModuleFragment, tracker: FactsTracker) {
        if (!tracker.isEnabled) return
        module.files.forEach { irFile ->
            val visited = hashSetOf<IrClass>()
            irFile.acceptChildrenVoid(object : IrTypeVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitType(container: IrElement, type: IrType) {
                    recordType(irFile, type, tracker, visited)
                }
            })
        }
    }

    /**
     * Records the uses of the value classes among the field types of [irClass]: the layout of a subclass of [irClass]
     * in [user] includes these fields, which [Fact.BaseClassLayout] of [irClass] records by signature only.
     */
    fun recordFieldsOf(user: IrFile, irClass: IrClass, tracker: FactsTracker) {
        if (!tracker.isEnabled) return
        val visited = hashSetOf<IrClass>()
        irClass.fieldTypes().forEach { recordType(user, it, tracker, visited) }
    }

    private fun recordType(user: IrFile, type: IrType, tracker: FactsTracker, visited: MutableSet<IrClass>) {
        val irClass = (type as? IrSimpleType)?.classifier?.owner as? IrClass ?: return
        if (!irClass.isValue || !visited.add(irClass)) return
        val file = irClass.fileOrNull
        // Facts of the same file are not recorded: a file is invalidated by its own definitions anyway.
        if (file != null && file != user && (irClass.symbol.signature ?: irClass.symbol.privateSignature) != null) {
            tracker.uses(user, Fact.BaseClassLayout.from(irClass))
        }
        // Even for a value class of the same file: its underlying type may come from another one.
        irClass.fieldTypes().forEach { recordType(user, it, tracker, visited) }
    }

    /** Same fields as the ones hashed by [Fact.BaseClassLayout]: still inside their properties before lowering. */
    private fun IrClass.fieldTypes(): List<IrType> = declarations.flatMap {
        when (it) {
            is IrField -> listOf(it.type)
            is IrProperty -> listOfNotNull(it.backingField?.type)
            else -> emptyList()
        }
    }
}
