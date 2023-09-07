/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.swift.CCode
import org.jetbrains.kotlin.backend.konan.swift.IrBasedSwiftGenerator
import org.jetbrains.kotlin.backend.konan.swift.SwiftCode
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.file.use
import java.io.File
import java.io.FileWriter

internal data class SwiftGenerationInput(val module: IrModuleFragment)

internal data class SwiftGenerationOutput(
        val moduleName: String,
        val swiftCodeProducer: () -> SwiftCode.File,
        val cCodeProducer: () -> CCode.File,
)

internal val SwiftGenerationPhase = createSimpleNamedCompilerPhase<PhaseContext, SwiftGenerationInput, SwiftGenerationOutput>(
        name = "SwiftGeneration",
        description = "Generate Swift API using backend IR",
        outputIfNotEnabled = { _, _, _, _ -> SwiftGenerationOutput("", { SwiftCode.File() }, { CCode.File(emptyList()) }) }
) { _, input ->
    val swiftGenerator = IrBasedSwiftGenerator()
    swiftGenerator.visitModuleFragment(input.module)
    SwiftGenerationOutput(
            input.module.name.asStringStripSpecialMarkers(),
            swiftGenerator::buildSwiftShimFile,
            swiftGenerator::buildSwiftBridgingHeader
    )
}

internal data class WriteSwiftGenerationInput(
        val moduleName: String,
        val swiftCodeProducer: () -> SwiftCode.File,
        val cCodeProducer: () -> CCode.File,
        val outputDirectory: File
)

internal val WriteSwiftGenerationOutputPhase = createSimpleNamedCompilerPhase<PhaseContext, WriteSwiftGenerationInput>(
        name = "WriteSwiftGenerationOutput",
        description = "Write Swift and Objective-C files to the output directory",
) { _, input ->
    File(input.outputDirectory.path, "${input.moduleName}.swift").let {
        it.createNewFile()
        FileWriter(it).use { writer ->
            input.swiftCodeProducer().renderLines().forEach { writer.write(it) }
        }
    }

    File(input.outputDirectory.path, "${input.moduleName}.h").let {
        it.createNewFile()
        FileWriter(it).use { writer ->
            input.cCodeProducer().renderLines().forEach { writer.write(it) }
        }
    }
}