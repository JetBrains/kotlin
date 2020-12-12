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
import java.security.MessageDigest
import java.util.ArrayList
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.util.KotlinJars
import kotlin.script.experimental.jvm.util.classpathFromClass
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import kotlin.script.experimental.jvmhost.JvmScriptCompiler

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
        checkWithCache(
            cache, scriptWithImport, scriptWithImportExpectedOutput,
            compilationConfiguration = { makeSimpleConfigurationWithTestImport() }
        )
    }


    @Test
    fun testFileCache() {
        withTempDir("scriptingTestCache") { cacheDir ->
            val cache = FileBasedScriptCache(cacheDir)
            Assert.assertEquals(true, cache.baseDir.listFiles()?.isEmpty())

            checkWithCache(cache, simpleScript, simpleScriptExpectedOutput)
        }
    }

    @Test
    fun testSimpleImportWithFileCache() {
        withTempDir("scriptingTestCache") { cacheDir ->
            val cache = FileBasedScriptCache(cacheDir)
            Assert.assertEquals(true, cache.baseDir.listFiles()?.isEmpty())

            checkWithCache(
                cache, scriptWithImport, scriptWithImportExpectedOutput,
                compilationConfiguration = { makeSimpleConfigurationWithTestImport() }
            )
        }
    }

    @Test
    fun testJarCache() {
        withTempDir("scriptingTestJarCache") { cacheDir ->
            val cache = TestCompiledScriptJarsCache(cacheDir)
            Assert.assertTrue(cache.baseDir.listFiles()!!.isEmpty())

            checkWithCache(cache, simpleScript, simpleScriptExpectedOutput)

            val scriptOut = runScriptFromJar(cache.baseDir.listFiles()!!.first { it.extension == "jar" })

            Assert.assertEquals(simpleScriptExpectedOutput, scriptOut)
        }
    }

    @Test
    fun testSimpleImportWithJarCache() {
        withTempDir("scriptingTestJarCache") { cacheDir ->
            val cache = TestCompiledScriptJarsCache(cacheDir)
            Assert.assertTrue(cache.baseDir.listFiles()!!.isEmpty())

            checkWithCache(
                cache, scriptWithImport, scriptWithImportExpectedOutput,
                compilationConfiguration = { makeSimpleConfigurationWithTestImport() }
            )

            // cannot make it work in this form - it requires a dependency on the current test classes, but classes directory seems
            // not work when specified in the manifest
            // TODO: find a way to make it work
//            val scriptOut = runScriptFromJar(cache.baseDir.listFiles()!!.first { it.extension == "jar" })
//
//            Assert.assertEquals(scriptWithImportExpectedOutput, scriptOut)
        }
    }

    @Test
    fun testImplicitReceiversWithJarCache() {
        withTempDir("scriptingTestJarCache") { cacheDir ->
            val cache = TestCompiledScriptJarsCache(cacheDir)
            Assert.assertTrue(cache.baseDir.listFiles()!!.isEmpty())

            checkWithCache(
                cache, simpleScript, simpleScriptExpectedOutput, checkDirectEval = false,
                compilationConfiguration = {
                    updateClasspath(classpathFromClass<ScriptingHostTest>()) // the class defined here should be in the classpath
                    implicitReceivers(Implicit::class)
                },
                evaluationConfiguration = {
                    implicitReceivers(Implicit)
                }
            )
        }
    }

    private fun checkWithCache(
        cache: ScriptingCacheWithCounters, script: String, expectedOutput: List<String>, checkDirectEval: Boolean = true,
        compilationConfiguration: ScriptCompilationConfiguration.Builder.() -> Unit = {},
        evaluationConfiguration: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
    ) {
        val myHostConfiguration = defaultJvmScriptingHostConfiguration.with {
            jvm {
                baseClassLoader.replaceOnlyDefault(null)
                compilationCache(cache)
            }
        }
        val compiler = JvmScriptCompiler(myHostConfiguration)
        val evaluator = BasicJvmScriptEvaluator()
        val host = BasicJvmScriptingHost(compiler = compiler, evaluator = evaluator)

        val scriptCompilationConfiguration = ScriptCompilationConfiguration(body = compilationConfiguration).with {
            updateClasspath(KotlinJars.kotlinScriptStandardJarsWithReflect)
            hostConfiguration.update { myHostConfiguration }
        }

        val scriptEvaluationConfiguration = ScriptEvaluationConfiguration(body = evaluationConfiguration)

        Assert.assertEquals(0, cache.storedScripts)
        var compiledScript: CompiledScript? = null
        val output = captureOut {
            runBlocking {
                compiler(script.toScriptSource(), scriptCompilationConfiguration).onSuccess {
                    compiledScript = it
                    evaluator(it, scriptEvaluationConfiguration)
                }.throwOnFailure()
            }
        }.lines()
        Assert.assertEquals(expectedOutput, output)
        Assert.assertEquals(1, cache.storedScripts)
        Assert.assertEquals(0, cache.retrievedScripts)

        if (checkDirectEval) {
            val cachedScript = cache.get(script.toScriptSource(), scriptCompilationConfiguration)
            Assert.assertNotNull(cachedScript)
            Assert.assertEquals(1, cache.retrievedScripts)

            val compiledScriptClassRes = runBlocking { compiledScript!!.getClass(null) }
            val cachedScriptClassRes = runBlocking { cachedScript!!.getClass(null) }

            val compiledScriptClass = compiledScriptClassRes.valueOrThrow()
            val cachedScriptClass = cachedScriptClassRes.valueOrThrow()

            Assert.assertEquals(compiledScriptClass.qualifiedName, cachedScriptClass.qualifiedName)
            Assert.assertEquals(compiledScriptClass.java.supertypes(), cachedScriptClass.java.supertypes())

            val output2 = captureOut {
                runBlocking {
                    evaluator(cachedScript!!, scriptEvaluationConfiguration).throwOnFailure()
                }
            }.lines()
            Assert.assertEquals(output, output2)
        }

        val output3 = captureOut {
            host.eval(script.toScriptSource(), scriptCompilationConfiguration, scriptEvaluationConfiguration).throwOnFailure()
        }.lines()
        Assert.assertEquals(if (checkDirectEval) 2 else 1, cache.retrievedScripts)
        Assert.assertEquals(output, output3)
    }
}

object Implicit

private interface ScriptingCacheWithCounters : CompiledJvmScriptsCache {

    val storedScripts: Int
    val retrievedScripts: Int
}

private class SimpleMemoryScriptsCache : ScriptingCacheWithCounters {

    internal val data = hashMapOf<Pair<SourceCode, Map<*, *>>, CompiledScript>()

    private var _storedScripts = 0
    private var _retrievedScripts = 0

    override val storedScripts: Int
        get() = _storedScripts

    override val retrievedScripts: Int
        get() = _retrievedScripts

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript? =
        data[script to scriptCompilationConfiguration.notTransientData]?.also { _retrievedScripts++ }

    override fun store(
        compiledScript: CompiledScript,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        data[script to scriptCompilationConfiguration.notTransientData] = compiledScript
        _storedScripts++
    }
}

private class FileBasedScriptCache(val baseDir: File) : ScriptingCacheWithCounters {

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript? {
        val file = File(baseDir, uniqueScriptHash(script, scriptCompilationConfiguration))
        return if (!file.exists()) null else file.readCompiledScript().also { retrievedScripts++ }
    }

    override fun store(
        compiledScript: CompiledScript,
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

class TestCompiledScriptJarsCache(val baseDir: File) :
    CompiledScriptJarsCache(
        { script, scriptCompilationConfiguration ->
            File(baseDir, uniqueScriptHash(script, scriptCompilationConfiguration) + ".jar")
        }
    ), ScriptingCacheWithCounters {

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript? =
        super.get(script, scriptCompilationConfiguration)?.also { retrievedScripts++ }

    override fun store(
        compiledScript: CompiledScript,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        super.store(compiledScript, script, scriptCompilationConfiguration).also { storedScripts++ }
    }

    override var storedScripts: Int = 0
        private set

    override var retrievedScripts: Int = 0
        private set
}

internal fun uniqueScriptHash(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
    val digestWrapper = MessageDigest.getInstance("MD5")
    digestWrapper.update(script.text.toByteArray())
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            digestWrapper.update(it.key.name.toByteArray())
            digestWrapper.update(it.value.toString().toByteArray())
        }
    return digestWrapper.digest().toHexString()
}

private fun File.readCompiledScript(): CompiledScript {
    return inputStream().use { fs ->
        ObjectInputStream(fs).use {
            it.readObject() as KJvmCompiledScript
        }
    }
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })

private fun Class<*>.supertypes(): MutableList<Class<*>> = when {
    superclass == null -> interfaces.toMutableList() ?: mutableListOf()
    interfaces.isEmpty() -> mutableListOf(superclass)
    else -> ArrayList<Class<*>>(interfaces.size + 1).apply {
        interfaces.toCollection(this@apply)
        add(superclass)
    }
}

