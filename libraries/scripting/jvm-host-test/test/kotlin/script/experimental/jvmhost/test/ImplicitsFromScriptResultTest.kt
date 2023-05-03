/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleInMemory
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvmhost.JvmScriptCompiler

/**
 * This test shows an ability of using KClasses loaded with classloaders
 * other than default one, in the role of implicit receivers. For this reason,
 * specific [GetScriptingClassByClassLoader] was implemented. Actually,
 * as we use previously compiled snippets as implicits, we could achieve the same
 * thing if we change the used compiler to one of the REPL ones. But if we are limited
 * in our choice of compiler and only can tune the configuration, this is the only way.
 *
 * This test may be deleted or at least simplified when the option
 * in [ScriptCompilationConfiguration] for saving previous classes in
 * underlying module will be introduced.
 */
class ImplicitsFromScriptResultTest : TestCase() {

    fun testImplicits() = expectTestToFailOnK2 {
        // the implementation of the Compiler Host doesn't work with IR - the inter-script symbol table
        // should be maintained to make it run (see latest REPL compiler implementations for details
        // TODO: consider either fix it or rewrite to the REPL compiler
        val host = CompilerHost()

        val snippets = listOf(
            "val xyz0 = 42",
            "fun f() = xyz0",
            "val finalRes = xyz0 + f()",
        )
        for (snippet in snippets) {
            val res = host.compile(snippet)
            assertTrue(res is ResultWithDiagnostics.Success)
        }
    }
}

fun interface PreviousScriptClassesProvider {
    fun get(): List<KClass<*>>
}

class GetScriptClassForImplicits(
    private val previousScriptClassesProvider: PreviousScriptClassesProvider
) : GetScriptingClassByClassLoader {
    private val getScriptingClass = JvmGetScriptingClass()

    private val lastClassLoader
        get() = previousScriptClassesProvider.get().lastOrNull()?.java?.classLoader

    override fun invoke(
        classType: KotlinType,
        contextClass: KClass<*>,
        hostConfiguration: ScriptingHostConfiguration
    ): KClass<*> {
        return getScriptingClass(classType, lastClassLoader ?: contextClass.java.classLoader, hostConfiguration)
    }

    override fun invoke(
        classType: KotlinType,
        contextClassLoader: ClassLoader?,
        hostConfiguration: ScriptingHostConfiguration
    ): KClass<*> {
        return getScriptingClass(classType, lastClassLoader ?: contextClassLoader, hostConfiguration)
    }
}

class CompilerHost {
    private var counter = 0
    private val implicits = mutableListOf<KClass<*>>()
    private val outputDir: Path = Files.createTempDirectory("kotlin-scripting-jvm")
    private val classWriter = ClassWriter(outputDir)

    init {
        outputDir.toFile().deleteOnExit()
    }

    private val myHostConfiguration = defaultJvmScriptingHostConfiguration.with {
        getScriptingClass(GetScriptClassForImplicits { getImplicitsClasses() })
    }

    private val compileConfiguration = ScriptCompilationConfiguration {
        hostConfiguration(myHostConfiguration)

        jvm {
            dependencies(JvmDependency(outputDir.toFile()))
        }
    }

    private val evaluationConfiguration = ScriptEvaluationConfiguration()

    private val compiler = JvmScriptCompiler(myHostConfiguration)

    private fun getImplicitsClasses(): List<KClass<*>> = implicits

    fun compile(code: String): ResultWithDiagnostics<CompiledScript> {
        val source = SourceCodeTestImpl(counter++, code)
        val refinedConfig = compileConfiguration.with {
            implicitReceivers(*implicits.toTypedArray())
        }
        val result = runBlocking { compiler.invoke(source, refinedConfig) }
        val compiledScript = result.valueOrThrow() as KJvmCompiledScript

        classWriter.writeCompiledSnippet(compiledScript)

        val kClass = runBlocking { compiledScript.getClass(evaluationConfiguration) }.valueOrThrow()
        implicits.add(kClass)
        return result
    }

    private class SourceCodeTestImpl(number: Int, override val text: String) : SourceCode {
        override val name: String = "Line_$number"
        override val locationId: String = "location_$number"
    }
}

class ClassWriter(private val outputDir: Path) {
    fun writeCompiledSnippet(snippet: KJvmCompiledScript) {
        val moduleInMemory = snippet.getCompiledModule() as KJvmCompiledModuleInMemory
        moduleInMemory.compilerOutputFiles.forEach { (name, bytes) ->
            if (name.endsWith(".class")) {
                writeClass(bytes, outputDir.resolve(name))
            }
        }
    }

    private fun writeClass(classBytes: ByteArray, path: Path) {
        FileOutputStream(path.toAbsolutePath().toString()).use { fos ->
            BufferedOutputStream(fos).use { out ->
                out.write(classBytes)
                out.flush()
            }
        }
    }
}
