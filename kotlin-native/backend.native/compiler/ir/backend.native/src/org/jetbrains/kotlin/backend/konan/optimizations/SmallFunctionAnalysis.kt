/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.*

internal object SmallFunctionAnalysis {
    fun findSmallFunction(context: Context, callGraph: CallGraph) {
        context.inVerbosePhase = true
        val condensation = DirectedGraphCondensationBuilder(callGraph).build()
        val smallFunctions = mutableSetOf<DataFlowIR.FunctionSymbol.Declared>()
        val predefined = listOf(
                context.irBuiltIns.anyClass.owner.constructors.single(),
                context.irBuiltIns.unitClass.owner.constructors.single()
        )
        smallFunctions.addAll(callGraph.directEdges.keys.filter { it.irFunction in predefined })
        var externalFunctions = 0
        var functionsWithLoops = 0
        var functionsWithAllocations = 0
        var functionsWithRecursiveCall = 0
        var functionsWithOtherNonSmallCall = 0
        var functionsWithVirtualCall = 0
        for (multiNode in condensation.topologicalOrder.reversed()) {
            val function = multiNode.nodes.singleOrNull()
            if (function == null || callGraph.directEdges[function]!!.callSites.any { it.actualCallee == function }) {
                functionsWithRecursiveCall += multiNode.nodes.size
                context.log { "Functions ${multiNode.nodes.joinToString { it.name!! }} are not small, as they are recursive" }
                continue
            }
            val irFunction = function.irFunction
            if (irFunction == null || irFunction.isExternal || irFunction.annotations.hasAnnotation(RuntimeNames.exportForCppRuntime)) {
                externalFunctions++
                context.log { "${function.name} is not small, as it is external" }
                continue
            }
            val loops = mutableListOf<IrLoop>()
            val constructorCalls = mutableListOf<IrConstructorCall>()
            irFunction.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    return element.acceptChildrenVoid(this)
                }

                override fun visitLoop(loop: IrLoop) {
                    loops.add(loop)
                }

                override fun visitConstructorCall(expression: IrConstructorCall) {
                    constructorCalls.add(expression)
                }
            })
            if (loops.isNotEmpty()) {
                functionsWithLoops++
                context.log { "${function.name} is not small, as it contain ${loops.size} loops" }
                continue
            }
            if (constructorCalls.isNotEmpty()) {
                functionsWithAllocations++
                context.log { "${function.name} is not small, as it allocates" }
                continue
            }
            val callSites = callGraph.directEdges[function]!!.callSites
            val virtualCall = callSites.firstOrNull { it.isVirtual }
            if (virtualCall != null) {
                functionsWithVirtualCall++
                context.log { "${function.name} is not small, as it has virtual call ${virtualCall.actualCallee.name}" }
                continue
            }
            val badCall = callSites.map { it.actualCallee }.firstOrNull { it !in smallFunctions || it.irFunction?.isExternal != false }
            if (badCall != null) {
                functionsWithOtherNonSmallCall++
                context.log { "${function.name} is not small, as it calls ${badCall.name}, which is not small" }
                continue
            }
            context.logMultiple {
                +"${function.name} is small!"
                +function.irFunction!!.dump()
            }
            smallFunctions.add(function)
        }
        context.smallFunctions = smallFunctions.map { it.irFunction!! }.toSet()
        context.logMultiple {
            +"Small function analysis stats:"
            +"  ${smallFunctions.size} functions are small"
            +"  $externalFunctions functions are not small as external"
            +"  $functionsWithLoops functions are not small as they have loops inside"
            +"  $functionsWithAllocations functions are not small as they have allocations inside"
            +"  $functionsWithVirtualCall functions are not small as they have virtual call inside"
            +"  $functionsWithOtherNonSmallCall functions are not small as they have call of other non-small functions inside"
        }
        context.inVerbosePhase = true
    }
}