/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.lower.bridgeTarget
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun dce(
        context: Context,
        irModule: IrModuleFragment,
        moduleDFG: ModuleDFG,
        devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult,
): Set<IrFunction> {
    val callGraph = CallGraphBuilder(
            context,
            irModule,
            moduleDFG,
            devirtualizationAnalysisResult,
            // Do not devirtualize anything to keep the graph smaller (albeit less precise which is fine for DCE).
            devirtualizedCallSitesUnfoldFactor = -1,
            nonDevirtualizedCallSitesUnfoldFactor = -1,
    ).build()

    val referencedFunctions = mutableSetOf<IrFunction>()
    callGraph.rootExternalFunctions.forEach {
        referencedFunctions.add(it.irFunction ?: error("No IR for: $it"))
    }
    for (node in callGraph.directEdges.values) {
        if (!node.symbol.isStaticFieldInitializer)
            referencedFunctions.add(node.symbol.irFunction ?: error("No IR for: ${node.symbol}"))
        node.callSites.forEach {
            if (!it.isVirtual)
                referencedFunctions.add(it.actualCallee.irFunction ?: error("No IR for: ${it.actualCallee}"))
        }
    }

    irModule.acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            // TODO: Generalize somehow, not that graceful.
            if (declaration.name == OperatorNameConventions.INVOKE
                    && declaration.parent.let { it is IrClass && it.defaultType.isFunction() }) {
                referencedFunctions.add(declaration)
            }
            super.visitFunction(declaration)
        }

        override fun visitConstructor(declaration: IrConstructor) {
            // TODO: NativePointed is the only inline class for which the field's type and
            //       the constructor parameter's type are different.
            //       Thus we need to conserve the constructor no matter if it was actually referenced somehow or not.
            //       See [IrTypeInlineClassesSupport.getInlinedClassUnderlyingType] why.
            if (declaration.parentAsClass.name.asString() == InteropFqNames.nativePointedName && declaration.isPrimary)
                referencedFunctions.add(declaration)
            super.visitConstructor(declaration)
        }
    })

    // Even if the target gets inlined into the bridge, RTTI generator needs the target to properly build vtables/itables.
    referencedFunctions.addAll(referencedFunctions.mapNotNull { it.bridgeTarget })

    irModule.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitFile(declaration: IrFile): IrFile {
            declaration.declarations.removeAll {
                (it is IrFunction && !referencedFunctions.contains(it))
            }
            return super.visitFile(declaration)
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            if (declaration == context.ir.symbols.nativePointed)
                return super.visitClass(declaration)
            declaration.declarations.removeAll {
                (it is IrFunction && it.isReal && !referencedFunctions.contains(it))
            }
            return super.visitClass(declaration)
        }

        override fun visitProperty(declaration: IrProperty): IrStatement {
            if (declaration.getter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                declaration.getter = null
            }
            if (declaration.setter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                declaration.setter = null
            }
            return super.visitProperty(declaration)
        }
    })

    return referencedFunctions
}

