/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.definitions.annotationsForSamWithReceivers
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvmhost.JvmScriptCompiler

class FeaturesTest : TestCase() {

    fun testSamWithReceiver() {
        withTempDir { tempDir ->
            runBlocking {
                val srcDir = File(TEST_DATA_DIR, "samWithReceiver")
                val destDir = File(tempDir, "dest").also { it.mkdir() }
                val javaRes = KotlinTestUtils.compileJavaFiles(
                    srcDir.listFiles { file: File -> file.extension == "java" }!!.toMutableList(),
                    mutableListOf("-d", destDir.absolutePath)
                )
                assertTrue(javaRes)

                val baseConfig = ScriptCompilationConfiguration {
                    fileExtension("samwr.kts")
                    dependencies(JvmDependency(destDir))
                }

                JvmScriptCompiler()(File(srcDir, "test.samwr.kts").toScriptSource(), baseConfig).let { res ->
                    when (res) {
                        is ResultWithDiagnostics.Success -> fail("Expecting \"Unresolved reference\" error, got successful compilation")
                        is ResultWithDiagnostics.Failure ->
                            if (res.reports.none { it.message.contains("Unresolved reference") || it.message.contains("'this' is not defined in this context") }) {
                                fail("Expecting \"Unresolved reference\" or \"'this' is not defined in this context\" error, got:\n  ${res.reports.joinToString("\n  ")}")
                            }
                    }
                }
                val configWithSwr = baseConfig.with {
                    annotationsForSamWithReceivers("SamWithReceiver1")
                }
                JvmScriptCompiler()(File(srcDir, "test.samwr.kts").toScriptSource(), configWithSwr).onFailure { res ->
                    fail("Compilation failed:\n  ${res.reports.joinToString("\n  ")}")
                }
            }
        }
    }
}