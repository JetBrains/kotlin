/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.impl

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.abi.tools.KlibDump
import org.jetbrains.kotlin.abi.tools.KlibTarget
import org.jetbrains.kotlin.abi.tools.impl.filtering.compileMatcher
import org.jetbrains.kotlin.abi.tools.impl.jvm.dump
import org.jetbrains.kotlin.abi.tools.impl.jvm.filterByMatcher
import org.jetbrains.kotlin.abi.tools.impl.klib.KlibDumpImpl
import org.jetbrains.kotlin.abi.tools.impl.jvm.loadApiFromJvmClasses
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile

internal object AbiToolsImpl : AbiTools {
    override fun <T : Appendable> printJvmDump(
        appendable: T,
        inputFiles: Iterable<File>,
        filters: AbiFilters,
    ) {
        val filtersMatcher = compileMatcher(filters)

        val signatures = streamsForInputFiles(inputFiles)
            .loadApiFromJvmClasses()
            .filterByMatcher(filtersMatcher)

        signatures.dump(appendable)
    }

    override fun createKlibDump(): KlibDump {
        return KlibDumpImpl()
    }

    override fun loadKlibDump(dumpFile: File): KlibDump {
        return KlibDumpImpl.from(dumpFile)
    }

    override fun loadKlibDump(dump: CharSequence): KlibDump {
        return KlibDumpImpl.from(dump)
    }

    override fun extractKlibAbi(
        klib: File,
        target: KlibTarget?,
        filters: AbiFilters,
    ): KlibDump {
        val dump = KlibDumpImpl.fromKlib(klib, filters)
        if (target != null) {
            dump.renameSingleTarget(target)
        }
        return dump
    }

    override fun filesDiff(expectedFile: File, actualFile: File): String? {
        val expectedText = expectedFile.readText()
        val actualText = actualFile.readText()

        // We don't compare a full text because newlines on Windows & Linux/macOS are different
        val expectedLines = expectedText.lines()
        val actualLines = actualText.lines()
        if (expectedLines == actualLines) {
            return null
        }

        val patch = DiffUtils.diff(expectedLines, actualLines)
        val diff =
            UnifiedDiffUtils.generateUnifiedDiff(expectedFile.toString(), actualFile.toString(), expectedLines, patch, 3)
        return diff.joinToString("\n")
    }

    private fun streamsFromJar(jarFile: File): Sequence<InputStream> {
        val jar = JarFile(jarFile)
        return jar.entries().iterator().asSequence()
            .filter { file ->
                !file.isDirectory && file.name.endsWith(".class") && !file.name.startsWith("META-INF/")
            }.map { entry -> jar.getInputStream(entry) }
    }

    private fun streamsForInputFiles(inputFiles: Iterable<File>): Sequence<InputStream> {
        return inputFiles.asSequence().flatMap { file ->
            if (!file.exists() || !file.isFile) {
                return@flatMap emptySequence()
            }
            when (file.extension) {
                "jar" -> streamsFromJar(file)
                "class" -> sequenceOf(file.inputStream())
                else -> emptySequence()
            }
        }
    }
}