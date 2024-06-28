/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.writer

import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.bridge.*
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportFiles
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div


internal fun SirModule.dumpResultToFiles(
    bridgeGenerator: BridgeGenerator,
    requests: List<BridgeRequest>,
    output: SwiftExportFiles,
    stableDeclarationsOrder: Boolean,
    renderDocComments: Boolean,
    additionalSwiftLinesProvider: () -> List<String>,
) {
    val cHeaderFile = output.cHeaderBridges.toFile()
    val ktBridgeFile = output.kotlinBridges.toFile()
    val swiftFile = output.swiftApi.toFile()

    val bridges = generateBridgeSources(bridgeGenerator, requests, stableDeclarationsOrder)
    val swiftSources = listOf(
        SirAsSwiftSourcesPrinter.print(
            this,
            stableDeclarationsOrder,
            renderDocComments
        )
    ) + additionalSwiftLinesProvider()
    dumpTextAtFile(bridges.ktSrc, ktBridgeFile)
    dumpTextAtFile(bridges.cSrc, cHeaderFile)
    dumpTextAtFile(swiftSources.asSequence(), swiftFile)
}

private fun generateBridgeSources(
    bridgeGenerator: BridgeGenerator,
    requests: List<BridgeRequest>,
    stableDeclarationsOrder: Boolean,
): BridgeSources {
    val kotlinBridgePrinter = createKotlinBridgePrinter()
    val cBridgePrinter = createCBridgePrinter()

    requests
        .let { if (stableDeclarationsOrder) it.sorted() else it }
        .flatMap(bridgeGenerator::generateFunctionBridges)
        .forEach {
            kotlinBridgePrinter.add(it)
            cBridgePrinter.add(it)
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
