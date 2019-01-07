/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import java.io.*
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.BasicScriptingHost
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvmhost.*
import kotlin.script.experimental.jvmhost.impl.CompiledScriptClassLoader
import kotlin.script.experimental.jvmhost.impl.KJvmCompiledScript
import kotlin.script.templates.standard.SimpleScriptTemplate

class ScriptingHostTest : TestCase() {

    companion object {
        const val TEST_DATA_DIR = "libraries/scripting/jvm-host/testData"
    }

    @Test
    fun testSimpleUsage() {
        val greeting = "Hello from script!"
        val output = captureOut {
            evalScript("println(\"$greeting\")")
        }
        Assert.assertEquals(greeting, output)
    }

    @Test
    fun testSimpleRequire() {
        val greeting = "Hello from required!"
        val script = "val subj = RequiredClass().value\nprintln(\"Hello from \$subj!\")"
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            importScripts(File(TEST_DATA_DIR, "importTest/requiredSrc.kt").toScriptSource())
        }
        val output = captureOut {
            BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration, null).throwOnFailure()
        }
        Assert.assertEquals(greeting, output)
    }

    @Test
    fun testSimpleImport() {
        val greeting = listOf("Hello from helloWithVal script!", "Hello from imported helloWithVal script!")
        val script = "println(\"Hello from imported \$helloScriptName script!\")"
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            refineConfiguration {
                beforeCompiling { ctx ->
                    val importedScript = File(TEST_DATA_DIR, "importTest/helloWithVal.kts")
                    if ((ctx.script as? FileScriptSource)?.file?.canonicalFile == importedScript.canonicalFile) {
                        ctx.compilationConfiguration
                    } else {
                        ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                            importScripts(importedScript.toScriptSource())
                        }
                    }.asSuccess()
                }
            }
        }
        val output = captureOut {
            BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration, null).throwOnFailure()
        }.lines()
        Assert.assertEquals(greeting, output)
    }

    @Test
    fun testImportError() {
        val script = "println(\"Hello from imported \$helloScriptName script!\")"
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            refineConfiguration {
                beforeCompiling { ctx ->
                    ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                        importScripts(File(TEST_DATA_DIR, "missing_script.kts").toScriptSource())
                    }.asSuccess()
                }
            }
        }
        val res = BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration, null)
        assertTrue(res is ResultWithDiagnostics.Failure)
        val report = res.reports.find { it.message.startsWith("Source file or directory not found") }
        assertNotNull(report)
        assertEquals("/script.kts", report?.sourcePath)
    }


    @Test
    fun testMemoryCache() {
        val script = "val x = 1\nprintln(\"x = \$x\")"
        val cache = SimpleMemoryScriptsCache()
        val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration, cache = cache)
        val evaluator = BasicJvmScriptEvaluator()
        val host = BasicJvmScriptingHost(compiler = compiler, evaluator = evaluator)
        Assert.assertTrue(cache.data.isEmpty())

        val output = captureOut { evalScript(script, host) }
        Assert.assertEquals("x = 1", output)

        Assert.assertEquals(1, cache.data.size)
        val compiled = cache.data.values.first()

        val output2 = captureOut { runBlocking { evaluator(compiled, null) } }
        Assert.assertEquals(output, output2)

        // TODO: check if cached script is actually used
        val output3 = captureOut { evalScript(script, host) }.trim()
        Assert.assertEquals(output, output3)
    }

    @Test
    fun testFileCache() {
        val script = "val x = 1\nprintln(\"x = \$x\")"
        val cacheDir = Files.createTempDirectory("scriptingTestCache").toFile()
        try {
            val cache = FileBasedScriptCache(cacheDir)
            val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration, cache = cache)
            val evaluator = BasicJvmScriptEvaluator()
            val host = BasicJvmScriptingHost(compiler = compiler, evaluator = evaluator)
            Assert.assertTrue(cache.baseDir.listFiles().isEmpty())

            val scriptCompilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate>()

            var compiledScript: CompiledScript<*>? = null
            val output = captureOut {
                runBlocking {
                    compiler(script.toScriptSource(), scriptCompilationConfiguration).onSuccess {
                        compiledScript = it
                        evaluator(it, null)
                    }.throwOnFailure()
                }
            }
            Assert.assertEquals("x = 1", output)

            val cachedCompiledScript = cache.baseDir.listFiles().let { files ->
                Assert.assertEquals(1, files.size)
                files.first().readCompiledScript(scriptCompilationConfiguration)
            }

            runBlocking {
                Assert.assertEquals(
                    compiledScript!!.getClass(null).resultOrNull()?.qualifiedName,
                    cachedCompiledScript.getClass(null).resultOrNull()?.qualifiedName
                )
            }

            val output2 = captureOut {
                runBlocking {
                    evaluator(cachedCompiledScript, null).throwOnFailure()
                }
            }
            Assert.assertEquals(output, output2)

            // TODO: check if cached script is actually used
            val output3 = captureOut { evalScript(script, host) }.trim()
            Assert.assertEquals(output, output3)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    @Test
    fun testCompiledScriptClassLoader() {
        val script = "val x = 1"
        val scriptCompilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate>()
        val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration)
        val compiledScript = runBlocking {
            val res = compiler(script.toScriptSource(), scriptCompilationConfiguration).throwOnFailure()
            (res as ResultWithDiagnostics.Success<CompiledScript<*>>).value
        }
        val compiledScriptClass = runBlocking { compiledScript.getClass(null).throwOnFailure().resultOrNull()!! as KClass<*> }
        val classLoader = compiledScriptClass.java.classLoader

        Assert.assertTrue(classLoader is CompiledScriptClassLoader)
        val anotherClass = classLoader.loadClass(compiledScriptClass.qualifiedName)

        Assert.assertEquals(compiledScriptClass.java, anotherClass)

        val classResourceName = compiledScriptClass.qualifiedName!!.replace('.', '/') + ".class"
        val classAsResourceUrl = classLoader.getResource(classResourceName)
        val classAssResourceStream = classLoader.getResourceAsStream(classResourceName)

        Assert.assertNotNull(classAsResourceUrl)
        Assert.assertNotNull(classAssResourceStream)

        val classAsResourceData = classAsResourceUrl.openConnection().getInputStream().readBytes()
        val classAsResourceStreamData = classAssResourceStream.readBytes()

        Assert.assertArrayEquals(classAsResourceData, classAsResourceStreamData)

        // TODO: consider testing getResources as well
    }
}

fun ResultWithDiagnostics<*>.throwOnFailure(): ResultWithDiagnostics<*> = apply {
    if (this is ResultWithDiagnostics.Failure) {
        val firstExceptionFromReports = reports.find { it.exception != null }?.exception
        throw Exception(
            "Compilation/evaluation failed:\n  ${reports.joinToString("\n  ") { it.exception?.toString() ?: it.message }}",
            firstExceptionFromReports
        )
    }
}

private fun evalScript(script: String, host: BasicScriptingHost = BasicJvmScriptingHost()) {
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate>()
    host.eval(script.toScriptSource(), compilationConfiguration, null).throwOnFailure()
}


private class SimpleMemoryScriptsCache : CompiledJvmScriptsCache {

    internal val data = hashMapOf<Pair<SourceCode, ScriptCompilationConfiguration>, CompiledScript<*>>()

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*>? =
        data[script to scriptCompilationConfiguration]

    override fun store(
        compiledScript: CompiledScript<*>,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        data[script to scriptCompilationConfiguration] = compiledScript
    }
}

private fun File.readCompiledScript(scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*> {
    return inputStream().use { fs ->
        ObjectInputStream(fs).use { os ->
            (os.readObject() as KJvmCompiledScript<*>).apply {
                setCompilationConfiguration(scriptCompilationConfiguration)
            }
        }
    }
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })


private class FileBasedScriptCache(val baseDir: File) : CompiledJvmScriptsCache {

    internal fun uniqueHash(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
        val digestWrapper = MessageDigest.getInstance("MD5")
        digestWrapper.update(script.text.toByteArray())
        scriptCompilationConfiguration.entries().sortedBy { it.key.name }.forEach {
            digestWrapper.update(it.key.name.toByteArray())
            digestWrapper.update(it.value.toString().toByteArray())
        }
        return digestWrapper.digest().toHexString()
    }

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*>? {
        val file = File(baseDir, uniqueHash(script, scriptCompilationConfiguration))
        return if (!file.exists()) null else file.readCompiledScript(scriptCompilationConfiguration)
    }

    override fun store(
        compiledScript: CompiledScript<*>,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        val file = File(baseDir, uniqueHash(script, scriptCompilationConfiguration))
        file.outputStream().use { fs ->
            ObjectOutputStream(fs).use { os ->
                os.writeObject(compiledScript)
            }
        }
    }
}

private fun captureOut(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return outStream.toString().trim()
}
