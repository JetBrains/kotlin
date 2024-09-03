/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package script

import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_PROPERTY
import org.jetbrains.kotlin.mainKts.MainKtsEvaluationConfiguration
import org.jetbrains.kotlin.mainKts.MainKtsScript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

fun evalFile(scriptFile: File, cacheDir: File? = null): ResultWithDiagnostics<EvaluationResult> =
    withMainKtsCacheDir(cacheDir?.absolutePath ?: "") {
        val scriptDefinition = createJvmCompilationConfigurationFromTemplate<MainKtsScript> {
            // Scripting doesn't support K2 yet.
            // See KT-64362
            compilerOptions("-language-version", "1.9")
        }

        val evaluationEnv = MainKtsEvaluationConfiguration.with {
            jvm {
                baseClassLoader(null)
            }
            constructorArgs(emptyArray<String>())
            enableScriptsInstancesSharing()
        }

        BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), scriptDefinition, evaluationEnv)
    }

const val TEST_DATA_ROOT = "testData"

class ComposeMainKtsTest {

    @Test
    fun testCompose() {
        val res = evalFile(File("$TEST_DATA_ROOT/compose.main.kts"))
        val returnValue: ResultValue = res.valueOrThrow().returnValue
        when(returnValue) {
            is ResultValue.Value -> {
                assertEquals(returnValue.value, "Hello World")
                assertEquals(returnValue.type, String::class.qualifiedName)
            }
            else -> fail("Unexpected eval result: $returnValue")
        }
        assertSucceeded(res)
    }

    private fun assertSucceeded(res: ResultWithDiagnostics<EvaluationResult>) {
        assertTrue(
            "test failed:\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Success
        )
    }

}

private fun <T> withMainKtsCacheDir(value: String?, body: () -> T): T {
    val prevCacheDir = System.getProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
    if (value == null) System.clearProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
    else System.setProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, value)
    try {
        return body()
    } finally {
        if (prevCacheDir == null) System.clearProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
        else System.setProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, prevCacheDir)
    }
}