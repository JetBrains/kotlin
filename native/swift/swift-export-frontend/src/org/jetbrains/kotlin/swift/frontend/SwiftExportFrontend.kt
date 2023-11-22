/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swift.frontend

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.analysisapi.SirFactory
import org.jetbrains.kotlin.sir.bridge.BridgeRequest
import org.jetbrains.kotlin.sir.bridge.createBridgeGenerator
import org.jetbrains.kotlin.sir.bridge.createCBridgePrinter
import org.jetbrains.kotlin.sir.bridge.createKotlinBridgePrinter
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.visitors.SirVisitor
import org.jetbrains.sir.passes.BodiesGenerationPass
import org.jetbrains.sir.passes.ForeignFunctionTranslatePass
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter

class SwiftExportFrontendOutput(
    val swiftSource: Sequence<String>,
    val kotlinSource: Sequence<String>,
    val header: Sequence<String>,
)

class SwiftExportFrontend(
    private val generator: SirFactory,
) {
    fun run(ktFiles: List<KtFile>): SwiftExportFrontendOutput {
        val module = buildModule {
            declarations += ktFiles.flatMap {
                generator.build(it)
            }
        }
        val passes = listOf(ForeignFunctionTranslatePass(), BodiesGenerationPass())
        passes.forEach {
            it.visitModule(module, Unit)
        }
        val requests = generateRequests(module)
        val bridgeGenerator = createBridgeGenerator()
        val cBridgePrinter = createCBridgePrinter()
        val kotlinPrinter = createKotlinBridgePrinter()

        requests.forEach { request ->
            val bridge = bridgeGenerator.generate(request)
            cBridgePrinter.add(bridge)
            kotlinPrinter.add(bridge)
        }

        val printer = SirAsSwiftSourcesPrinter
        val moduleSB = StringBuilder()
        printer.visitModule(module, moduleSB)
        return SwiftExportFrontendOutput(moduleSB.lineSequence(), kotlinPrinter.print(), cBridgePrinter.print())
    }

    private fun generateRequests(module: SirModule): List<BridgeRequest> {
        val requests = mutableListOf<BridgeRequest>()

        fun bridgeType(sirType: SirType): BridgeRequest.Type {
            if (sirType !is SirNominalType) error("")
            return when (sirType.declaration) {
                is BuiltinSirTypeDeclaration.Bool -> BridgeRequest.Type.Boolean
                is BuiltinSirTypeDeclaration.Int8 -> BridgeRequest.Type.Byte
                is BuiltinSirTypeDeclaration.Int16 -> BridgeRequest.Type.Short
                is BuiltinSirTypeDeclaration.Int32 -> BridgeRequest.Type.Int
                is BuiltinSirTypeDeclaration.Int64 -> BridgeRequest.Type.Long
                else -> error("")
            }
        }

        module.acceptChildren(object: SirVisitor<Unit, MutableList<BridgeRequest>>() {
            override fun visitElement(element: SirElement, data: MutableList<BridgeRequest>) {

            }

            override fun visitFunction(function: SirFunction, data: MutableList<BridgeRequest>) {
                val body = function.body ?: return
                val origin = function.origin as? KotlinFunctionSirOrigin ?: return
                val params = function.parameters.map { sirParam ->
                    BridgeRequest.Parameter(sirParam.argumentName ?: "", bridgeType(sirParam.type))
                }
                val request = BridgeRequest(origin.fqName, body.bridgeName, params, bridgeType(function.returnType))
                data += request
            }
        }, requests)
        return requests.toList()
    }
}