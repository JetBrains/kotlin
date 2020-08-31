/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.internal.visitor

import java.io.File
import java.io.ObjectOutputStream
import java.io.Serializable
import java.lang.StringBuilder
import java.util.*
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.impl.deserializer
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit

internal class ScriptCreator(
    private val persistentValuesSerializer: FileBasedPersistentValuesSerializer,
    private val importsCollector: ImportCollector
) : GeneratedCodeVisitor by GeneratedCodeVisitor(importsCollector, persistentValuesSerializer) {
    private val stringBuilder = StringBuilder()

    override fun writeScript(build: StringBuilder.() -> Unit) {
        stringBuilder.build()
    }

    fun build(directory: File): SourceCode? {
        if (stringBuilder.isBlank()) return null

        persistentValuesSerializer.build(directory)?.let { visit(it, 0) }

        val string = buildString {
            for (import in importsCollector.build()) {
                appendLine("import $import")
            }

            appendLine()
            append(stringBuilder)
        }

        return File(directory, "Provided.kts").apply {
            if (!exists()) createNewFile()
            writeText(string)

            // TODO: Caching
            deleteOnExit()
        }.toScriptSource()
    }
}

internal class ImportCollector : GeneratedCodeVisitor {
    private val imports = mutableSetOf<String>()

    override fun useImport(import: String) {
        imports.add(import)
    }

    fun build(): Set<String> {
        return imports
    }
}

internal class ScriptCollector : GeneratedCodeVisitor {
    private val sourceCode = mutableListOf<SourceCode>()

    override fun includeScript(code: SourceCode) {
        sourceCode.add(code)
    }

    fun build(): List<SourceCode> {
        return sourceCode
    }
}

private const val allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

internal class FileBasedPersistentValuesSerializer : GeneratedCodeVisitor {
    private val values = mutableMapOf<String, Any>()

    override fun withSerialized(value: Any, block: (String) -> Unit) {
        val id = generateID()
        values[id] = value
        block(id)
    }

    fun build(directory: File): GeneratedCode? {
        if (values.isEmpty()) return null

        val file = File(directory, "persistedData").apply {
            if (!exists())
                createNewFile()

            // TODO: Caching
            deleteOnExit()
        }

        ObjectOutputStream(file.outputStream()).use { it.writeObject(values) }

        return file.deserializer()
    }

    private fun generateID(): String {
        var id: String
        do {
            id = generateRandomString()
        } while (values.containsKey(id))
        return id
    }

    private fun generateRandomString() = Random()
        .ints(20, 0, allowedCharacters.length)
        .toArray()
        .map { allowedCharacters[it] }
        .joinToString("")
}