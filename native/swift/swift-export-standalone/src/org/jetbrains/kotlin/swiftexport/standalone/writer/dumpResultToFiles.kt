/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.writer

import org.jetbrains.kotlin.sir.bridge.*
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportFiles
import java.io.File

internal fun dumpTextAtPath(
    swift: Sequence<String>,
    bridges: BridgeSources,
    output: SwiftExportFiles
) {
    dumpTextAtFile(bridges.ktSrc, output.kotlinBridges.toFile())
    dumpTextAtFile(bridges.cSrc, output.cHeaderBridges.toFile())
    dumpTextAtFile(swift, output.swiftApi.toFile())
}

internal fun generateBridgeSources(
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

internal fun dumpTextAtFile(text: Sequence<String>, file: File) {
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


internal data class BridgeSources(val ktSrc: Sequence<String>, val cSrc: Sequence<String>)
