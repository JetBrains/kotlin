/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.ir.backend.js.loadKlib
import org.jetbrains.kotlin.ir.backend.js.getModuleDescriptorByLibrary
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.resolve.ScriptLightVirtualFile
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.*
import java.nio.charset.Charset
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.jvm.JsDependency

fun getScriptKtFile(
    script: SourceCode,
    scriptText: String,
    project: Project
): ResultWithDiagnostics<KtFile> {
    val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
    val virtualFile = ScriptLightVirtualFile(
        script.name!!,
        (script as? FileBasedScriptSource)?.file?.path,
        scriptText
    )
    val ktFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
    return when {
        ktFile == null -> ResultWithDiagnostics.Failure(
            ScriptDiagnostic(
                message = "Cannot create PSI",
                severity = ScriptDiagnostic.Severity.ERROR
            )
        )
        ktFile.declarations.firstIsInstanceOrNull<KtScript>() == null -> ResultWithDiagnostics.Failure(
            ScriptDiagnostic(
                message = "There is not Script",
                severity = ScriptDiagnostic.Severity.ERROR
            )
        )
        else -> ktFile.asSuccess()
    }
}

fun makeReplCodeLine(no: Int, code: String): ReplCodeLine = ReplCodeLine(no, 0, code)

//TODO: remove and use collector from kotlin-scripting-compiler
class ReplMessageCollector : MessageCollector {
    private var hasErrors = false
    private var messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()

    override fun clear() {
        hasErrors = false
        messages.clear()
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        if (severity == CompilerMessageSeverity.ERROR) hasErrors = true
        messages.add(Pair(severity, message))
    }

    override fun hasErrors(): Boolean {
        return hasErrors
    }

    fun hasNotErrors(): Boolean {
        return !hasErrors
    }

    fun getMessage(): String {
        val resultMessage = StringBuilder("Found ${messages.size} problems:\n")
        for (m in messages) {
            resultMessage.append(m.first.toString() + " : " + m.second + "\n")
        }
        return resultMessage.toString()
    }
}

fun readLibrariesFromConfiguration(configuration: CompilerConfiguration): List<ModuleDescriptor> {
    val scriptConfig = configuration[ScriptingConfigurationKeys.SCRIPT_DEFINITIONS]!!
    val scriptCompilationConfig = scriptConfig.find { (it).platform == "JS" }!!.compilationConfiguration
    val scriptDependencies = scriptCompilationConfig[ScriptCompilationConfiguration.dependencies]!!
    return scriptDependencies.map { loadKlib((it as JsDependency).path) }.map { getModuleDescriptorByLibrary(it) }
}

fun createCompileResult(code: String) = createCompileResult(LineId(ReplCodeLine(0, 0, "")), code)

fun createCompileResult(lineId: LineId, code: String): ReplCompileResult.CompiledClasses {
    return ReplCompileResult.CompiledClasses(
        lineId,
        emptyList(),
        "",
        emptyList(),
        false,
        emptyList(),
        "Any?",
        code
    )
}

class DependencyLoader {
    private val commonPath = "compiler/ir/serialization.js/build/fullRuntime/klib"
    private val mappedNamesPath = "$commonPath/mappedNames.txt"
    private val scriptDependencyBinaryPath = "$commonPath/scriptDependencyBinary.js"

    fun saveNames(nameTables: NameTables, path: String = mappedNamesPath) {
        writeDataByPath(writeNames(nameTables), path)
    }

    fun loadNames(path: String = mappedNamesPath): NameTables {
        return readNames(readDataByPath(path))
    }

    fun saveScriptDependencyBinary(stdlibCompiledResult: String, path: String = scriptDependencyBinaryPath) {
        writeDataByPath(writeScriptDependencyBinary(stdlibCompiledResult), path)
    }

    fun loadScriptDependencyBinary(path: String = scriptDependencyBinaryPath): String {
        return readScriptDependencyBinary(readDataByPath(path))
    }


    fun writeNames(nameTables: NameTables): ByteArray {
        val result = StringBuilder()
        for (entry in nameTables.mappedNames) {
            result.append("${entry.key} ${entry.value}" + System.lineSeparator())
        }
        return result.toString().toByteArray(Charset.defaultCharset())
    }

    fun readNames(data: ByteArray): NameTables {
        val mappedNames = mutableMapOf<String, String>()
        val reserved = mutableSetOf<String>()

        BufferedReader(InputStreamReader(data.inputStream())).use { reader ->
            for (line in reader.readLines()) {
                val (key, value) = line.split(" ")
                mappedNames[key] = value
                reserved += value
            }
        }

        return NameTables(emptyList(), mappedNames = mappedNames, reservedForGlobal = reserved)
    }

    fun writeScriptDependencyBinary(stdlibCompiledResult: String): ByteArray {
        return stdlibCompiledResult.toByteArray(Charset.defaultCharset())
    }

    fun readScriptDependencyBinary(data: ByteArray): String {
        return data.toString(Charset.defaultCharset())
    }

    fun readDataByPath(path: String): ByteArray {
        FileReader(path).use { reader ->
            val stdlibCompiledResult = reader.readText()
            return stdlibCompiledResult.toByteArray(Charset.defaultCharset())
        }
    }

    fun writeDataByPath(data: ByteArray, path: String) {
        FileOutputStream(path).use {
            it.write(data)
        }
    }
}
