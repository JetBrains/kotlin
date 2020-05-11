/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.messageCollectorLogger
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE_ALL
import java.io.File

class ApiTest : KotlinTestWithEnvironment() {

    private val STDLIB_PATH = "js/js.translator/testData/api/stdlib"

    fun testStdlib() {
        val project = environment.project
        val configuration = environment.configuration

        configuration.put(CommonConfigurationKeys.MODULE_NAME, "test")
        configuration.put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB)

        val config = JsConfig(project, configuration)

        config.moduleDescriptors.single().checkRecursively(STDLIB_PATH)
    }

    private val STDLIB_IR_PATH = "js/js.translator/testData/api/stdlib-ir"

    fun testIrStdlib() {
        val fullRuntimeKlib: String = System.getProperty("kotlin.js.full.stdlib.path")

        val resolvedLibraries =
            jsResolveLibraries(listOf(File(fullRuntimeKlib).absolutePath), messageCollectorLogger(MessageCollector.NONE))

        val project = environment.project
        val configuration = environment.configuration

        val moduleDescriptor = loadIr(
            project,
            MainModule.Klib(resolvedLibraries.getFullList().single()),
            AnalyzerWithCompilerReport(configuration),
            configuration,
            resolvedLibraries,
            listOf()
        ).module.descriptor

        moduleDescriptor.checkRecursively(STDLIB_IR_PATH)
    }

    private val STDLIB_DIFF_PATH = "js/js.translator/testData/api/stdlib-diff"
    private val onlyInStdlib = setOf(
        "org.khronos.webgl",
        "org.w3c.css.masking",
        "org.w3c.dom",
        "org.w3c.dom.clipboard",
        "org.w3c.dom.css",
        "org.w3c.dom.encryptedmedia",
        "org.w3c.dom.events",
        "org.w3c.dom.mediacapture",
        "org.w3c.dom.mediasource",
        "org.w3c.dom.parsing",
        "org.w3c.dom.pointerevents",
        "org.w3c.dom.svg",
        "org.w3c.dom.url",
        "org.w3c.fetch",
        "org.w3c.files",
        "org.w3c.notifications",
        "org.w3c.performance",
        "org.w3c.workers",
        "org.w3c.xhr"
    )
    private val onlyInStdlibIr = setOf("testUtils")

    fun testApiDifference() {
        val files = STDLIB_PATH.listFiles()
        val irFiles = STDLIB_IR_PATH.listFiles()

        val allNames = (files + irFiles).map { it.name.dropLast(3).split('-').first() }.toSet()

        for (name in allNames) {
            val a = STDLIB_PATH.readFileText(name)
            val b = STDLIB_IR_PATH.readFileText(name)

            if (a == null) {
                assertTrue("Package '$name' unexpectedly only present in IR stdlib", name in onlyInStdlibIr)
            } else if (b == null) {
                assertTrue("Package '$name' unexpectedly only present in old stdlib", name in onlyInStdlib)
            } else {
                val d = diff(a, b)

                if (d.isNotBlank()) {
                    // Uncomment to overwrite the test data
//                    File("$STDLIB_DIFF_PATH/$name.kt").writeText(d)
                    KotlinTestUtils.assertEqualsToFile(File("$STDLIB_DIFF_PATH/$name.kt"), d)
                }
            }
        }
    }

    private fun String.readFileText(name: String): String? {
        val f = File("$this/$name.kt")
        if (f.exists() && f.isFile) {
            return f.readText()
        } else {
            var i = 0
            var result: String? = null
            while (true) {
                val f = File("$this/$name-$i.kt")
                if (!f.exists() || !f.isFile) break;

                result = (result ?: "") + f.readText()

                ++i
            }

            return result
        }
    }

    private fun diff(a: String, b: String): String {
        val aLines = a.lines()
        val bLines = b.lines()

        val dx = Array(aLines.size + 1) { IntArray(bLines.size + 1) }
        val dy = Array(aLines.size + 1) { IntArray(bLines.size + 1)}
        val c = Array(aLines.size + 1) { IntArray(bLines.size + 1)}

        for (i in 0..aLines.size) {
            c[i][0] = i
            dx[i][0] = -1
        }
        for (j in 0..bLines.size) {
            c[0][j] = j
            dy[0][j] = -1
        }
        for (i in 1..aLines.size) {
            for (j in 1..bLines.size) {
                if (c[i - 1][j] <= c[i][j - 1]) {
                    c[i][j] = c[i - 1][j] + 1
                    dx[i][j] = -1
                } else {
                    c[i][j] = c[i][j - 1] + 1
                    dy[i][j] = -1
                }
                if (aLines[i - 1] == bLines[j - 1] && c[i - 1][j - 1] < c[i][j]) {
                    c[i][j] = c[i - 1][j - 1]
                    dx[i][j] = -1
                    dy[i][j] = -1
                }
            }
        }

        val result = mutableListOf<String>()

        var x = aLines.size
        var y = bLines.size
        var hasDiff = false

        while (x != 0 && y != 0) {
            val tdx = dx[x][y]
            val tdy = dy[x][y]

            if (tdx != 0) {
                if (tdy != 0) {
                    if (hasDiff) {
                        result += "--- ${x + 1},${y + 1} ---"
                        hasDiff = false
                    }
                } else {
                    result += "- ${aLines[x - 1]}"
                    hasDiff = true
                }
            } else if (tdy != 0) {
                result += "+ ${bLines[y - 1]}"
                hasDiff = true
            }

            x += tdx
            y += tdy
        }

        return result.reversed().joinToString("\n", postfix = "\n")
    }

    private fun String.listFiles(): Array<File> {
        val dirFile = File(this)
        assertTrue("Directory does not exist: ${dirFile.absolutePath}", dirFile.exists())
        assertTrue("Not a directory: ${dirFile.absolutePath}", dirFile.isDirectory)
        return dirFile.listFiles()!!
    }

    private fun ModuleDescriptor.checkRecursively(dir: String) {
        val dirFile = File(dir)
        assertTrue("Directory does not exist: ${dirFile.absolutePath}", dirFile.exists())
        assertTrue("Not a directory: ${dirFile.absolutePath}", dirFile.isDirectory)
        val files = dirFile.listFiles()!!.map { it.name }.toMutableSet()
        allPackages().forEach { fqName ->
            getPackage(fqName).serialize()?.let { serialized ->
                serialized.forEachIndexed { index, part ->

                    val fileName =
                        (if (fqName.isRoot) "ROOT" else fqName.asString()) + (if (serialized.size == 1) "" else "-$index") + ".kt"
                    files -= fileName

                    // Uncomment to overwrite the test data
//                    File("$dir/$fileName").writeText(part)
                    KotlinTestUtils.assertEqualsToFile(File("$dir/$fileName"), part)
                }
            }
        }

        assertTrue("Extra files found: ${files}", files.isEmpty())
    }

    private fun ModuleDescriptor.allPackages(): Collection<FqName> {
        val result = mutableListOf<FqName>()

        fun impl(pkg: FqName) {
            result += pkg

            getSubPackagesOf(pkg) { true }.forEach { impl(it) }
        }

        impl(FqName.ROOT)

        return result
    }

    private fun PackageViewDescriptor.serialize(): List<String>? {
        val comparator = RecursiveDescriptorComparator(RECURSIVE_ALL.filterRecursion {
            when {
                it is MemberDescriptor && it.isExpect -> false
                it is DeclarationDescriptorWithVisibility && !it.visibility.isPublicAPI -> false
                it is CallableMemberDescriptor && !it.kind.isReal -> false
                it is PackageViewDescriptor -> false
                else -> true
            }
        }.renderDeclarationsFromOtherModules(true))

        val serialized = comparator.serializeRecursively(this).trim()

        val lines = serialized.lines()

        if (lines.size <= 1) return null

        if (lines.size > LINES_PER_FILE_CUTOFF) {
            val result = mutableListOf<String>()

            val sb = StringBuilder()
            var cnt = 0
            var bracketBalance = 0

            for (d in lines) {
                if (cnt > LINES_PER_FILE_CUTOFF && bracketBalance == 0 && (d.isBlank() || !d[0].isWhitespace())) {
                    result += sb.toString()
                    sb.clear()
                    cnt = 0
                }

                sb.append(d).append('\n')
                ++cnt
                bracketBalance += d.count { it == '{' } - d.count { it == '}' }
            }

            if (sb.isNotBlank()) {
                result += sb.toString()
            }

            return result
        }

        return listOf(serialized)
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(TestDisposable(), CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    }
}

// IDEA isn't able to show diff for files that are too big
private val LINES_PER_FILE_CUTOFF = 1000