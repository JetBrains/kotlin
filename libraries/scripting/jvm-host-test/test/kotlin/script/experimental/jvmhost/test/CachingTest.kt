/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvmhost.*
import kotlin.script.templates.standard.SimpleScriptTemplate

class CachingTest : TestCase() {

    val simpleScript = "val x = 1\nprintln(\"x = \$x\")"
    val simpleScriptExpectedOutput = listOf("x = 1")

    val scriptWithImport = "println(\"Hello from imported \$helloScriptName script!\")"
    val scriptWithImportExpectedOutput = listOf("Hello from helloWithVal script!", "Hello from imported helloWithVal script!")

    @Test
    fun testMemoryCache() {
        val cache = SimpleMemoryScriptsCache()
        checkWithCache(cache, simpleScript, simpleScriptExpectedOutput)
    }

    @Test
    fun testSimpleImportWithMemoryCache() {
        val cache = SimpleMemoryScriptsCache()
        checkWithCache(cache, scriptWithImport, scriptWithImportExpectedOutput) { makeSimpleConfigurationWithTestImport() }
    }


    @Test
    fun testFileCache() {
        withTempDir("scriptingTestCache") { cacheDir ->
            val cache = FileBasedScriptCache(cacheDir)
            Assert.assertTrue(cache.baseDir.listFiles().isEmpty())

            checkWithCache(cache, simpleScript, simpleScriptExpectedOutput)
        }
    }

    @Test
    fun testSimpleImportWithFileCache() {
        withTempDir("scriptingTestCache") { cacheDir ->
            val cache = FileBasedScriptCache(cacheDir)
            Assert.assertTrue(cache.baseDir.listFiles().isEmpty())

            checkWithCache(cache, scriptWithImport, scriptWithImportExpectedOutput) { makeSimpleConfigurationWithTestImport() }
        }
    }

    private fun checkWithCache(
        cache: ScriptingCacheWithCounters, script: String, expectedOutput: List<String>,
        configurationBuilder: ScriptCompilationConfiguration.Builder.() -> Unit = {}
    ) {
        val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration, cache = cache)
        val evaluator = BasicJvmScriptEvaluator()
        val host = BasicJvmScriptingHost(compiler = compiler, evaluator = evaluator)

        val scriptCompilationConfiguration =
            createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate>(body = configurationBuilder)

        Assert.assertEquals(0, cache.storedScripts)
        var compiledScript: CompiledScript<*>? = null
        val output = captureOut {
            runBlocking {
                compiler(script.toScriptSource(), scriptCompilationConfiguration).onSuccess {
                    compiledScript = it
                    evaluator(it)
                }.throwOnFailure()
            }
        }.lines()
        Assert.assertEquals(expectedOutput, output)
        Assert.assertEquals(1, cache.storedScripts)
        Assert.assertEquals(0, cache.retrievedScripts)

        val cachedScript = cache.get(script.toScriptSource(), scriptCompilationConfiguration)
        Assert.assertNotNull(cachedScript)
        Assert.assertEquals(1, cache.retrievedScripts)

        val compiledScriptClassRes = runBlocking { compiledScript!!.getClass(null) }
        val cachedScriptClassRes = runBlocking { cachedScript!!.getClass(null) }

        val compiledScriptClass = compiledScriptClassRes.valueOrNull()
        val cachedScriptClass = cachedScriptClassRes.valueOrNull()

        Assert.assertEquals(compiledScriptClass!!.qualifiedName, cachedScriptClass!!.qualifiedName)
        Assert.assertEquals(compiledScriptClass!!.supertypes, cachedScriptClass!!.supertypes)

        val output2 = captureOut {
            runBlocking {
                evaluator(cachedScript!!).throwOnFailure()
            }
        }.lines()
        Assert.assertEquals(output, output2)

        val output3 = captureOut { evalScriptWithConfiguration(script, host, configurationBuilder).throwOnFailure() }.lines()
        Assert.assertEquals(2, cache.retrievedScripts)
        Assert.assertEquals(output, output3)
    }
}

private interface ScriptingCacheWithCounters : CompiledJvmScriptsCache {

    val storedScripts: Int
    val retrievedScripts: Int
}

private class SimpleMemoryScriptsCache : ScriptingCacheWithCounters {

    internal val data = hashMapOf<Pair<SourceCode, ScriptCompilationConfiguration>, CompiledScript<*>>()

    private var _storedScripts = 0
    private var _retrievedScripts = 0

    override val storedScripts: Int
        get() = _storedScripts

    override val retrievedScripts: Int
        get() = _retrievedScripts

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*>? =
        data[script to scriptCompilationConfiguration]?.also { _retrievedScripts++ }

    override fun store(
        compiledScript: CompiledScript<*>,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        data[script to scriptCompilationConfiguration] = compiledScript
        _storedScripts++
    }
}

private class FileBasedScriptCache(val baseDir: File) : ScriptingCacheWithCounters {

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*>? {
        val file = File(baseDir, uniqueScriptHash(script, scriptCompilationConfiguration))
        return if (!file.exists()) null else file.readCompiledScript(scriptCompilationConfiguration)?.also { retrievedScripts++ }
    }

    override fun store(
        compiledScript: CompiledScript<*>,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        val file = File(baseDir, uniqueScriptHash(script, scriptCompilationConfiguration))
        file.outputStream().use { fs ->
            ObjectOutputStream(fs).use { os ->
                os.writeObject(compiledScript)
            }
        }
        storedScripts++
    }

    override var storedScripts: Int = 0
        private set

    override var retrievedScripts: Int = 0
        private set
}

internal fun uniqueScriptHash(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
    val digestWrapper = MessageDigest.getInstance("MD5")
    digestWrapper.update(script.text.toByteArray())
    scriptCompilationConfiguration.entries().sortedBy { it.key.name }.forEach {
        digestWrapper.update(it.key.name.toByteArray())
        digestWrapper.update(it.value.toString().toByteArray())
    }
    return digestWrapper.digest().toHexString()
}

private fun File.readCompiledScript(scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*> {
    return inputStream().use { fs ->
        ObjectInputStream(fs).use {
            it.readObject() as KJvmCompiledScript<*>
        }
    }
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })

private fun <R> withTempDir(keyName: String = "tmp", body: (File) -> R) {
    val tempDir = Files.createTempDirectory(keyName).toFile()
    try {
        body(tempDir)
    } finally {
        tempDir.deleteRecursively()
    }
}

