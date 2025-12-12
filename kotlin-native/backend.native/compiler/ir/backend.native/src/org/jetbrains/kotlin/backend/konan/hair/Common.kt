/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.hair

import hair.compilation.Compilation
import hair.compilation.Config
import hair.compilation.HairDumper
import org.jetbrains.kotlin.backend.common.reportCompilationWarning
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal fun createHairCompilation(context: Context, module: IrModuleFragment): Compilation {
    val config = Config(
            hairDumper = createHairDumper(context, module)
    )

    return Compilation(config)
}

private fun createHairDumper(context: Context, module: IrModuleFragment): HairDumper? {
    val baseDumpDir = context.config.dumpHairTo ?: return null
    val compilationDumpDir = baseDumpDir.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss--SSS")))
    val moduleDumpDir = compilationDumpDir.resolve(module.name.toString())
    val created = moduleDumpDir.mkdirs()
    if (!created) {
        context.reportCompilationWarning("Failed to create HaIR dump directory: $moduleDumpDir")
        return null
    }
    return object : HairDumper() {
        val unitDumpCounters = mutableMapOf<String, Int>()
        override fun dumpImpl(unitName: String, title: String, contents: String) {
            // FIXME mangle some stuff??
            val unitDumpDir = moduleDumpDir.resolve(unitName)
            unitDumpDir.mkdirs()
            if (unitDumpDir.exists()) {
                val dumpNumber = unitDumpCounters.getOrPut(unitName) { 0 }
                unitDumpCounters[unitName] = dumpNumber + 1
                val dumpTitle = "${dumpNumber}_${title}"
                unitDumpDir.resolve("$dumpTitle.dot").writeText(contents)
            }
        }
    }
}