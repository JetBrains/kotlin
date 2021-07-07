/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.CompiledScriptClassLoader
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmCompiledModuleInMemoryImpl
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.BasicScriptingHost
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.KotlinJars
import kotlin.script.experimental.jvm.util.classpathFromClass
import kotlin.script.experimental.jvmhost.*
import kotlin.script.templates.standard.SimpleScriptTemplate

class ScriptingHostTest : TestCase() {

    @Test
    fun testSimpleUsage() {
        val greeting = "Hello from script!"
        val output = captureOut {
            evalScript("println(\"$greeting\")").throwOnFailure()
        }
        Assert.assertEquals(greeting, output)
        // another API
        val output2 = captureOut {
            BasicJvmScriptingHost().evalWithTemplate<SimpleScriptTemplate>("println(\"$greeting\")".toScriptSource()).throwOnFailure()
        }
        Assert.assertEquals(greeting, output2)
    }

    @Test
    fun testSourceWithName() {
        val greeting = "Hello from script!"
        val output = captureOut {
            val basicJvmScriptingHost = BasicJvmScriptingHost()
            basicJvmScriptingHost.evalWithTemplate<SimpleScript>(
                "println(\"$greeting\")".toScriptSource("name"),
                compilation = {
                    updateClasspath(classpathFromClass<SimpleScript>())
                }
            ).throwOnFailure()
        }
        Assert.assertEquals(greeting, output)
    }

    @Test
    fun testValueResult() {
        val evalScriptWithResult = evalScriptWithResult("42")
        val resVal = evalScriptWithResult as ResultValue.Value
        Assert.assertEquals(42, resVal.value)
        Assert.assertEquals("\$\$result", resVal.name)
        Assert.assertEquals("kotlin.Int", resVal.type)
        val resField = resVal.scriptInstance!!::class.java.getDeclaredField("\$\$result")
        Assert.assertEquals(42, resField.get(resVal.scriptInstance!!))
    }

    @Test
    fun testUnitResult() {
        val resVal = evalScriptWithResult("val x = 42")
        Assert.assertTrue(resVal is ResultValue.Unit)
    }

    @Test
    fun testErrorResult() {
        val resVal = evalScriptWithResult("throw RuntimeException(\"abc\")")
        Assert.assertTrue(resVal is ResultValue.Error)
        val resValError = (resVal as ResultValue.Error).error
        Assert.assertTrue(resValError is RuntimeException)
        Assert.assertEquals("abc", resValError.message)
    }

    @Test
    fun testCustomResultField() {
        val resVal = evalScriptWithResult("42") {
            resultField("outcome")
        } as ResultValue.Value
        Assert.assertEquals("outcome", resVal.name)
        val resField = resVal.scriptInstance!!::class.java.getDeclaredField("outcome")
        Assert.assertEquals(42, resField.get(resVal.scriptInstance!!))
    }

    @Test
    fun testSaveToClasses() {
        val greeting = "Hello from script classes!"
        val outDir = Files.createTempDirectory("saveToClassesOut").toFile()
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate>()
        val host = BasicJvmScriptingHost(evaluator = BasicJvmScriptClassFilesGenerator(outDir))
        host.eval("println(\"$greeting\")".toScriptSource(name = "SavedScript.kts"), compilationConfiguration, null).throwOnFailure()
        val classloader = URLClassLoader(arrayOf(outDir.toURI().toURL()), ScriptingHostTest::class.java.classLoader)
        val scriptClass = classloader.loadClass("SavedScript")
        val output = captureOut {
            scriptClass.newInstance()
        }
        Assert.assertEquals(greeting, output)
    }

    @Test
    fun testSaveToJar() {
        val greeting = "Hello from script jar!"
        val outJar = Files.createTempFile("saveToJar", ".jar").toFile()
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate>()
        val host = BasicJvmScriptingHost(evaluator = BasicJvmScriptJarGenerator(outJar))
        host.eval("println(\"$greeting\")".toScriptSource(name = "SavedScript.kts"), compilationConfiguration, null).throwOnFailure()
        Thread.sleep(100)
        val classloader = URLClassLoader(arrayOf(outJar.toURI().toURL()), ScriptingHostTest::class.java.classLoader)
        val scriptClass = classloader.loadClass("SavedScript")
        val output = captureOut {
            scriptClass.newInstance()
        }
        Assert.assertEquals(greeting, output)
    }

    @Test
    fun testSaveToRunnableJar() {
        val greeting = "Hello from script jar!"
        val outJar = Files.createTempFile("saveToRunnableJar", ".jar").toFile()
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate>() {
            updateClasspath(classpathFromClass<SimpleScriptTemplate>())
            updateClasspath(KotlinJars.kotlinScriptStandardJarsWithReflect)
        }
        val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration)
        val scriptName = "SavedRunnableScript"
        val compiledScript = runBlocking {
            compiler("println(\"$greeting\")".toScriptSource(name = "$scriptName.kts"), compilationConfiguration).throwOnFailure()
                .valueOrNull()!!
        }
        val saver = BasicJvmScriptJarGenerator(outJar)
        runBlocking {
            saver(compiledScript, ScriptEvaluationConfiguration.Default).throwOnFailure()
        }

        Thread.sleep(100)

        val classpathFromJar = run {
            val manifest = JarFile(outJar).manifest
            manifest.mainAttributes.getValue("Class-Path").split(" ") // TODO: quoted paths
                .map { File(it).toURI().toURL() }
        } + outJar.toURI().toURL()

        fun checkInvokeMain(baseClassLoader: ClassLoader?) {
            val classloader = URLClassLoader(classpathFromJar.toTypedArray(), baseClassLoader)
            val scriptClass = classloader.loadClass(scriptName)
            val mainMethod = scriptClass.methods.find { it.name == "main" }
            Assert.assertNotNull(mainMethod)
            val output = captureOutAndErr {
                mainMethod!!.invoke(null, emptyArray<String>())
            }.toList().filterNot(String::isEmpty).joinToString("\n")
            Assert.assertEquals(greeting, output)
        }

        checkInvokeMain(null) // isolated
        checkInvokeMain(Thread.currentThread().contextClassLoader)

        val outputFromProcess = runScriptFromJar(outJar)
        Assert.assertEquals(listOf(greeting), outputFromProcess)
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
            makeSimpleConfigurationWithTestImport()
        }
        val output = captureOut {
            BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration, null).throwOnFailure()
        }.lines()
        Assert.assertEquals(greeting, output)
    }

    @Test
    fun testSimpleImportWithImplicitReceiver() {
        val greeting = listOf("Hello from helloWithVal script!", "Hello from imported helloWithVal script!")
        val script = "println(\"Hello from imported \$helloScriptName script!\")"
        val definition = createJvmScriptDefinitionFromTemplate<SimpleScriptTemplate>(
            compilation = {
                makeSimpleConfigurationWithTestImport()
                implicitReceivers(String::class)
            },
            evaluation = {
                implicitReceivers("abc")
            }
        )
        val output = captureOut {
            BasicJvmScriptingHost().eval(
                script.toScriptSource(), definition.compilationConfiguration, definition.evaluationConfiguration
            ).throwOnFailure()
        }.lines()
        Assert.assertEquals(greeting, output)
    }

    @Test
    fun testDiamondImportWithoutSharing() {
        val greeting = listOf("Hi from common", "Hi from middle", "Hi from common", "sharedVar == 3")
        val output = doDiamondImportTest()
        Assert.assertEquals(greeting, output)
    }

    @Test
    fun testDiamondImportWithSharing() {
        val greeting = listOf("Hi from common", "Hi from middle", "sharedVar == 5")
        val output = doDiamondImportTest(
            ScriptEvaluationConfiguration {
                enableScriptsInstancesSharing()
            }
        )
        Assert.assertEquals(greeting, output)
    }

    private fun doDiamondImportTest(evaluationConfiguration: ScriptEvaluationConfiguration? = null): List<String> {
        val mainScript = "sharedVar += 1\nprintln(\"sharedVar == \$sharedVar\")".toScriptSource("main.kts")
        val middleScript = File(TEST_DATA_DIR, "importTest/diamondImportMiddle.kts").toScriptSource()
        val commonScript = File(TEST_DATA_DIR, "importTest/diamondImportCommon.kts").toScriptSource()
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            refineConfiguration {
                beforeCompiling { ctx ->
                    when (ctx.script.name) {
                        "main.kts" -> ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                            importScripts(middleScript, commonScript)
                        }
                        "diamondImportMiddle.kts" -> ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                            importScripts(commonScript)
                        }
                        else -> ctx.compilationConfiguration
                    }.asSuccess()
                }
            }
        }
        val output = captureOut {
            BasicJvmScriptingHost().eval(mainScript, compilationConfiguration, evaluationConfiguration).throwOnFailure()
        }.lines()
        return output
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
        assertEquals("script.kts", report?.sourcePath)
    }

    @Test
    fun testCompileOptionsLanguageVersion() {
        val script = "fun interface FunInterface {\n    fun invoke()\n}"
        val compilationConfiguration1 = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            compilerOptions("-language-version", "1.3")
        }
        val res = BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration1, null)
        assertTrue(res is ResultWithDiagnostics.Failure)
        res.reports.find { it.message.startsWith("The feature \"functional interface conversion\" is only available since language version 1.4") }
            ?: fail("Error report about language version not found. Reported:\n  ${res.reports.joinToString("\n  ") { it.message }}")
    }

    @Test
    fun testCompileOptionsNoStdlib() {
        val script = "println(\"Hi\")"

        val res1 = evalScriptWithConfiguration(script) {
            compilerOptions("-no-stdlib")
        }
        assertTrue(res1 is ResultWithDiagnostics.Failure)
        res1.reports.find { it.message.startsWith("Unresolved reference: println") }
            ?: fail("Expected unresolved reference report. Reported:\n  ${res1.reports.joinToString("\n  ") { it.message }}")

        val res2 = evalScriptWithConfiguration(script) {
            refineConfiguration {
                beforeCompiling { ctx ->
                    ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                        compilerOptions("-no-stdlib")
                    }.asSuccess()
                }
            }
        }
        // -no-stdlib in refined configuration has no effect
        assertTrue(res2 is ResultWithDiagnostics.Success)
    }

    @Test
    fun testErrorOnParsingOptions() {
        val script = "println(\"Hi\")"

        val compilationConfiguration1 = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            compilerOptions("-jvm-target->1.8")
        }
        val res1 = BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration1, null)
        assertTrue(res1 is ResultWithDiagnostics.Failure)
        assertNotNull(res1.reports.find { it.message == "Invalid argument: -jvm-target->1.8" })

        val compilationConfiguration2 = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            refineConfiguration {
                beforeCompiling { ctx ->
                    ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                        compilerOptions.append("-jvm-target->1.6")
                    }.asSuccess()
                }
            }
        }
        val res2 = BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration2, null)
        assertTrue(res2 is ResultWithDiagnostics.Failure)
        assertNotNull(res2.reports.find { it.message == "Invalid argument: -jvm-target->1.6" })
    }

    @Test
    fun testInvalidOptionsWarning() {
        val script = "1"
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            compilerOptions("-Xunknown1")
            refineConfiguration {
                beforeCompiling { ctx ->
                    ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                        compilerOptions.append("-Xunknown2")
                    }.asSuccess()
                }
            }
        }
        val res = BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration, null)
        assertTrue(res is ResultWithDiagnostics.Success)
        assertNotNull(res.reports.find { it.message == "Flag is not supported by this version of the compiler: -Xunknown1" })
        assertNotNull(res.reports.find { it.message == "Flag is not supported by this version of the compiler: -Xunknown2" })
    }

    @Test
    fun testIgnoredOptionsWarning() {
        val script = "println(\"Hi\")"
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            compilerOptions("-version", "-d", "destDir", "-Xreport-perf", "-no-reflect")
            refineConfiguration {
                beforeCompiling { ctx ->
                    ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                        compilerOptions.append("-no-jdk", "-version", "-no-stdlib", "-Xdump-perf", "-no-inline")
                    }.asSuccess()
                }
            }
        }
        val res = BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration, null)
        assertTrue(res is ResultWithDiagnostics.Success)
        assertNotNull(res.reports.find { it.message == "The following compiler arguments are ignored on script compilation: -version, -d, -Xreport-perf" })
        assertNotNull(res.reports.find { it.message == "The following compiler arguments are ignored on script compilation: -Xdump-perf" })
        assertNotNull(res.reports.find { it.message == "The following compiler arguments are ignored when configured from refinement callbacks: -no-jdk, -no-stdlib" })
    }

    fun jvmTargetTestImpl(target: String, expectedVersion: Int) {
        val script = "println(\"Hi\")"
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate> {
            compilerOptions("-jvm-target", target)
        }
        val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration)
        val compiledScript = runBlocking { compiler(script.toScriptSource(name = "SavedScript.kts"), compilationConfiguration) }
        assertTrue(compiledScript is ResultWithDiagnostics.Success)

        val jvmCompiledScript = compiledScript.valueOrNull()!! as KJvmCompiledScript
        val jvmCompiledModule = jvmCompiledScript.getCompiledModule() as KJvmCompiledModuleInMemoryImpl
        val bytes = jvmCompiledModule.compilerOutputFiles["SavedScript.class"]!!

        var classFileVersion: Int? = null
        ClassReader(bytes).accept(object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visit(
                version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?
            ) {
                classFileVersion = version
            }
        }, 0)

        assertEquals(expectedVersion, classFileVersion)
    }

    @Test
    fun testJvmTarget() {
        jvmTargetTestImpl("1.6", 50)
        jvmTargetTestImpl("1.8", 52)
        jvmTargetTestImpl("9", 53)
    }

    @Test
    fun testCompiledScriptClassLoader() {
        val script = "val x = 1"
        val scriptCompilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate>()
        val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration)
        val compiledScript = runBlocking {
            val res = compiler(script.toScriptSource(), scriptCompilationConfiguration).throwOnFailure()
            (res as ResultWithDiagnostics.Success<CompiledScript>).value
        }
        val compiledScriptClass = runBlocking { compiledScript.getClass(null).throwOnFailure().valueOrNull()!! }
        val classLoader = compiledScriptClass.java.classLoader

        Assert.assertTrue(classLoader is CompiledScriptClassLoader)
        val anotherClass = classLoader.loadClass(compiledScriptClass.qualifiedName)

        Assert.assertEquals(compiledScriptClass.java, anotherClass)

        val classResourceName = compiledScriptClass.qualifiedName!!.replace('.', '/') + ".class"
        val classAsResourceUrl = classLoader.getResource(classResourceName)
        val classAssResourceStream = classLoader.getResourceAsStream(classResourceName)

        Assert.assertNotNull(classAsResourceUrl)
        Assert.assertNotNull(classAssResourceStream)

        val classAsResourceData = classAsResourceUrl!!.openConnection().getInputStream().readBytes()
        val classAsResourceStreamData = classAssResourceStream!!.readBytes()

        Assert.assertArrayEquals(classAsResourceData, classAsResourceStreamData)

        // TODO: consider testing getResources as well
    }
}

internal fun runScriptFromJar(jar: File): List<String> {
    val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")
    val args = listOf(javaExecutable.absolutePath, "-jar", jar.path)
    val processBuilder = ProcessBuilder(args)
    processBuilder.redirectErrorStream(true)
    val r = run {
        val process = processBuilder.start()
        process.waitFor(10, TimeUnit.SECONDS)
        val out = process.inputStream.reader().readText()
        if (process.isAlive) {
            process.destroyForcibly()
            "Error: timeout, killing script process\n$out"
        } else {
            out
        }
    }.trim()
    return r.lineSequence().map { it.trim() }.toList()
}

fun <T> ResultWithDiagnostics<T>.throwOnFailure(): ResultWithDiagnostics<T> = apply {
    if (this is ResultWithDiagnostics.Failure) {
        val firstExceptionFromReports = reports.find { it.exception != null }?.exception
        throw Exception(
            "Compilation/evaluation failed:\n  ${reports.joinToString("\n  ") { it.exception?.toString() ?: it.message }}",
            firstExceptionFromReports
        )
    }
}

private fun evalScript(script: String, host: BasicScriptingHost = BasicJvmScriptingHost()): ResultWithDiagnostics<*> =
    evalScriptWithConfiguration(script, host)

private fun evalScriptWithResult(
    script: String,
    host: BasicScriptingHost = BasicJvmScriptingHost(),
    body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ResultValue =
    evalScriptWithConfiguration(script, host, body).throwOnFailure().valueOrNull()!!.returnValue

internal fun evalScriptWithConfiguration(
    script: String,
    host: BasicScriptingHost = BasicJvmScriptingHost(),
    body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ResultWithDiagnostics<EvaluationResult> {
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScriptTemplate>(body = body)
    return host.eval(script.toScriptSource(), compilationConfiguration, null)
}

internal fun ScriptCompilationConfiguration.Builder.makeSimpleConfigurationWithTestImport() {
    updateClasspath(classpathFromClass<ScriptingHostTest>()) // the lambda below should be in the classpath
    refineConfiguration {
        beforeCompiling { ctx ->
            val importedScript = File(TEST_DATA_DIR, "importTest/helloWithVal.kts")
            if ((ctx.script as? FileBasedScriptSource)?.file?.canonicalFile == importedScript.canonicalFile) {
                ctx.compilationConfiguration
            } else {
                ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                    importScripts(importedScript.toScriptSource())
                }
            }.asSuccess()
        }
    }
}

internal fun captureOut(body: () -> Unit): String = captureOutAndErr(body).first

internal fun captureOutAndErr(body: () -> Unit): Pair<String, String> {
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()
    val prevOut = System.out
    val prevErr = System.err
    System.setOut(PrintStream(outStream))
    System.setErr(PrintStream(errStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.err.flush()
        System.setOut(prevOut)
        System.setErr(prevErr)
    }
    return outStream.toString().trim() to errStream.toString().trim()
}

