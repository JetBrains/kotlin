/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.writer

import org.jetbrains.kotlin.sir.bridge.*
import org.jetbrains.kotlin.swiftexport.compilerconfig.CompilerConfig
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportFiles
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable

internal fun dumpResultToFiles(
    swift: Sequence<String>,
    bridges: BridgeSources,
    compilerConfig: CompilerConfig,
    output: SwiftExportFiles,
) {
    dumpTextAtFile(bridges.ktSrc, output.kotlinBridges.toFile())
    dumpTextAtFile(bridges.cSrc, output.cHeaderBridges.toFile())
    dumpTextAtFile(swift, output.swiftApi.toFile())
    output.compilerConfig.toFile().run {
        ensureExists()
        compilerConfig.writeToFile(this)
    }
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

private fun File.ensureExists() {
    if (!exists()) {
        parentFile.mkdirs()
        createNewFile()
    }
}

internal fun dumpTextAtFile(text: Sequence<String>, file: File) {
    file.ensureExists()
    val writer = file.printWriter()
    for (t in text) {
        writer.println(t)
    }
    writer.close()
}


internal data class BridgeSources(val ktSrc: Sequence<String>, val cSrc: Sequence<String>)
