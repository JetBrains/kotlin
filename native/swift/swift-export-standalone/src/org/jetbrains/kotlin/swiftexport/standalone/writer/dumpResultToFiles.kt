/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.writer

import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.bridge.BridgeRequest
import org.jetbrains.kotlin.sir.bridge.createBridgeGenerator
import org.jetbrains.kotlin.sir.bridge.createCBridgePrinter
import org.jetbrains.kotlin.sir.bridge.createKotlinBridgePrinter
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportOutput
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import java.io.File


internal fun SirModule.dumpResultToFiles(requests: List<BridgeRequest>, output: SwiftExportOutput) {
    val cHeaderFile = output.cHeaderBridges.toFile()
    val ktBridgeFile = output.kotlinBridges.toFile()
    val swiftFile = output.swiftApi.toFile()

    val bridges = generateBridgeSources(requests)
    val swiftSrc = generateSwiftSrc()

    dumpTextAtFile(bridges.ktSrc, ktBridgeFile)
    dumpTextAtFile(bridges.cSrc, cHeaderFile)
    dumpTextAtFile(sequenceOf(swiftSrc), swiftFile)
}

private fun SirModule.generateSwiftSrc(): String {
    return SirAsSwiftSourcesPrinter().print(this)
}

private fun generateBridgeSources(requests: List<BridgeRequest>): BridgeSources {

    val generator = createBridgeGenerator()
    val kotlinBridgePrinter = createKotlinBridgePrinter()
    val cBridgePrinter = createCBridgePrinter()

    requests.forEach { request ->
        val bridge = generator.generate(request)
        kotlinBridgePrinter.add(bridge)
        cBridgePrinter.add(bridge)
    }

    val actualKotlinSrc = kotlinBridgePrinter.print()
    val actualCHeader = cBridgePrinter.print()

    return BridgeSources(ktSrc = actualKotlinSrc, cSrc = actualCHeader)
}

private fun dumpTextAtFile(text: Sequence<String>, file: File) {
    if (!file.exists()) {
        file.parentFile.mkdirs()
        file.createNewFile()
    }
    val writer = file.printWriter()
    for (t in text) {
        writer.println(t)
    }
    writer.close()
}


private data class BridgeSources(val ktSrc: Sequence<String>, val cSrc: Sequence<String>)
