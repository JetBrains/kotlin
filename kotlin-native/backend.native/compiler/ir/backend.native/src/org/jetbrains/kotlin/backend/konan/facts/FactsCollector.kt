/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import org.jetbrains.kotlin.backend.konan.ir.BridgesPolicy
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.objcinterop.externalObjCClassFqName
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.parentDeclarationsWithSelf
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Computes the facts a file defines from its IR as deserialized from the klib, that is, before any lowering.
 * The [FactsCollector] is a helper object used during the dirt-set computations.
 * Assuming we already ran the compiler once, we would have `uses_{n-1}` registered. The computation of dirt-set
 * relies on the `uses_{n-1}` and `defs_n`.
 *
 * Both the compiler and [KlibFactsGenerator] use it, so the facts of a klib can be computed without compiling it
 * and compared against the ones stored by the previous compilation.
 */
internal object FactsCollector {

    fun collectDefsFrom(irModules: List<IrModuleFragment>, tracker: FactsTracker, bridgesPolicy: BridgesPolicy) {
        irModules.forEach { module ->
            module.files.forEach { collectDefsFrom(it, tracker, bridgesPolicy) }
        }
    }

    fun collectDefsFrom(irFile: IrFile, tracker: FactsTracker, bridgesPolicy: BridgesPolicy) {
        if (!tracker.isEnabled) return

        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                val signature = declaration.symbol.signature ?: declaration.symbol.privateSignature
                if (!declaration.isExternalObjC && signature != null) {
                    tracker.defines(irFile, Fact.BaseClassLayout.from(declaration))
                    tracker.defines(irFile, Fact.methodTableLayoutOf(declaration, bridgesPolicy))
                }
                if (declaration.isEnumClass) {
                    tracker.defines(irFile, Fact.EnumEntries.from(declaration))
                }
                super.visitClass(declaration)
            }

            override fun visitField(declaration: IrField) {
                val signature = declaration.symbol.signature ?: declaration.symbol.privateSignature
                if (signature != null) {
                    tracker.defines(irFile, Fact.Signature.from(declaration))
                }
                super.visitField(declaration)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                val signature = declaration.symbol.signature ?: declaration.symbol.privateSignature
                if (signature != null) {
                    tracker.defines(irFile, Fact.Signature.from(declaration))
                }
                super.visitConstructor(declaration)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                val signature = declaration.symbol.signature ?: declaration.symbol.privateSignature
                if (!declaration.isFakeOverride && signature != null) {
                    tracker.defines(irFile, Fact.Signature.from(declaration))
                    if (declaration.isInline && declaration.body != null) {
                        tracker.defines(irFile, Fact.InlineBody.from(declaration))
                    }
                }
                super.visitSimpleFunction(declaration)
            }

            override fun visitProperty(declaration: IrProperty) {
                val signature = declaration.symbol.signature ?: declaration.symbol.privateSignature
                // The initializer may be missing e.g. for an expect constant, which defines no such fact.
                if (declaration.isConst && signature != null && declaration.backingField?.initializer?.expression != null) {
                    tracker.defines(irFile, Fact.ConstInitializer.from(declaration))
                }
                super.visitProperty(declaration)
            }
        })
    }

    // Same classes as `requiresRtti()`, without walking the supertypes: in [KlibFactsGenerator] the ones from other klibs are unbound.
    private val IrClass.isExternalObjC: Boolean
        get() = parentDeclarationsWithSelf.filterIsInstance<IrClass>().any { it.hasAnnotation(externalObjCClassFqName) }
}
