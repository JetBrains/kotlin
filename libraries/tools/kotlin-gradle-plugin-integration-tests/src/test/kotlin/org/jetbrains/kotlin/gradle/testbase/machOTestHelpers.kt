/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.test.assertTrue

internal data class MachOSymbol(val type: String, val name: String) {
    /** Debug map (STABS) entries are printed by `nm -a` with a `-` type. */
    val isDebugMapEntry get() = type == "-"

    /** `nm` prints global symbols in upper case and non-global ones in lower case. */
    val isLocal get() = type.first().isLowerCase()
}

internal fun TestProject.machOSymbols(binary: Path): List<MachOSymbol> =
    runTool("nm", "-a", "-P", binary.pathString).mapNotNull { line ->
        // '-P' prints '<name> <type> <value> <size>'; a name may contain spaces, so take the fields from the right
        val fields = line.split(" ")
        if (fields.size < 4) return@mapNotNull null
        MachOSymbol(type = fields[fields.size - 3], name = fields.dropLast(3).joinToString(" "))
    }

internal fun TestProject.machOSectionNames(binary: Path): List<String> =
    runTool("otool", "-l", binary.pathString).mapNotNull {
        it.trim().substringAfter("sectname ", missingDelimiterValue = "").ifEmpty { null }
    }

internal fun TestProject.runTool(vararg command: String): List<String> {
    val result = runProcess(command.toList(), projectPath.toFile())
    result.assertProcessRunResult { assertTrue(isSuccessful, "'${command.joinToString(" ")}' failed") }
    return result.output.lines()
}
