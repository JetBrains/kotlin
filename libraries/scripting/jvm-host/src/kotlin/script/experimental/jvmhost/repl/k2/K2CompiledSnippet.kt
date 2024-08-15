/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl.k2

import java.io.File
import java.io.IOException
import kotlin.reflect.KClass
import kotlin.script.experimental.api.CompiledSnippet
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.asSuccess

/**
 * Custom class loader making it easier to load snippets we manually compiled.
 * This class is temporary while we iterate on the K2 REPL API.
 */
class ReplSnippetClassLoader(
    parent: ClassLoader,
    val buildDir: File
): ClassLoader(parent) {
    override fun findClass(name: String): Class<*> {
        if (!name.startsWith("repl.snippet")) {
            return super.findClass(name)
        }
        val fileName = name.split(".").last()
        val classFile = File(buildDir, fileName + ".class")
        if (!classFile.exists()) {
            throw ClassNotFoundException("Class not found: $name")
        }
        try {
            val bytes = classFile.readBytes()
            return defineClass(name, bytes, 0, bytes.size)
        } catch (e: IOException) {
            throw ClassNotFoundException("Class loading failed for: $name", e)
        }
    }
}

/**
 * Dummy class only used for experimentation, while the final API for K2 Repl is being
 * developed.
 */
class K2CompiledSnippet(private val buildDir: File, private val classFQN: String) : CompiledSnippet {

    override val compilationConfiguration: ScriptCompilationConfiguration
        get() = TODO("Not yet implemented")

    override suspend fun getClass(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<KClass<*>> {
        // Custom class loader for loading our compiled script source
        // Note, the parent class loader logic is probably horribly wrong, but should work
        // for the context of the tests
        val classLoader = ReplSnippetClassLoader(this::class.java.classLoader, buildDir)
        return try {
            val clazz = classLoader.loadClass(classFQN).kotlin
            clazz.asSuccess()
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(
                ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedError,
                    "Unable to instantiate class $classFQN: ${e.message}",
                    sourcePath = sourceLocationId,
                    exception = e
                )
            )
        }
    }
}
