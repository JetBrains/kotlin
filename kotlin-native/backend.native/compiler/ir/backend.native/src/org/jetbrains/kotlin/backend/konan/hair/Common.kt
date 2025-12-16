/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.hair

import hair.compilation.Compilation
import hair.compilation.Config
import hair.compilation.HairDumper
import hair.sym.HairFunction
import org.jetbrains.kotlin.backend.common.reportCompilationWarning
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.backend.konan.llvm.computeFunctionName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.name.Name
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
        val unitDumpCounters = mutableMapOf<IrFunction, Int>()
        override fun dumpImpl(f: HairFunction, title: String, contents: String) {
            // FIXME mangle some stuff??
            val irFunction = (f as HairFunctionImpl).irFunction;
            val nameFragments = irFunction.computeFullNameFragments()
            val unitDumpDir = moduleDumpDir.resolve(nameFragments.joinToString(File.separator))
            unitDumpDir.mkdirs()
            if (unitDumpDir.exists()) {
                val dumpNumber = unitDumpCounters.getOrPut(irFunction) { 0 }
                unitDumpCounters[irFunction] = dumpNumber + 1
                val dumpTitle = "${dumpNumber}_${title}"
                unitDumpDir.resolve("$dumpTitle.dot").writeText(contents)
            }
        }
    }
}

private fun IrFunction.computeFullNameFragments(): List<String> =
        (parent.fqNameForIrSerialization.asString().split(".") + listOf(computeFunctionName())).filter { it.isNotEmpty() }

