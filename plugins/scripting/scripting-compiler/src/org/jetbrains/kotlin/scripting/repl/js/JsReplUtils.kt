/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.compiler.wjs.ModuleLoader
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.resolve.ScriptLightVirtualFile
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.FileReader
import java.io.InputStreamReader
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
                ScriptDiagnostic.unspecifiedError,
                message = "Cannot create PSI",
                severity = ScriptDiagnostic.Severity.ERROR
            )
        )
        ktFile.declarations.firstIsInstanceOrNull<KtScript>() == null -> ResultWithDiagnostics.Failure(
            ScriptDiagnostic(
                ScriptDiagnostic.unspecifiedError,
                message = "There is not Script",
                severity = ScriptDiagnostic.Severity.ERROR
            )
        )
        else -> ktFile.asSuccess()
    }
}

private fun createBuiltIns(storageManager: StorageManager) = object : KotlinBuiltIns(storageManager) {}
private val JsFactories = KlibMetadataFactories(::createBuiltIns, DynamicTypeDeserializer)

private fun getModuleDescriptorByLibrary(current: KotlinLibrary, mapping: Map<String, ModuleDescriptorImpl>): ModuleDescriptorImpl {
    val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
        current,
        LanguageVersionSettingsImpl.DEFAULT,
        LockBasedStorageManager.NO_LOCKS,
        null,
        packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
        lookupTracker = LookupTracker.DO_NOTHING
    )

    val dependencies = current.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { mapping.getValue(it) }

    md.setDependencies(listOf(md) + dependencies)
    return md
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

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
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

fun readLibrariesFromConfiguration(configuration: CompilerConfiguration): Collection<ModuleDescriptor> {
    val scriptConfig = configuration[ScriptingConfigurationKeys.SCRIPT_DEFINITIONS]!!
    val scriptCompilationConfig = scriptConfig.find { (it).platform == "JS" }!!.compilationConfiguration
    val scriptDependencies = scriptCompilationConfig[ScriptCompilationConfiguration.dependencies]!!
    val libraries = scriptDependencies.map { (it as JsDependency).path }
    val logger = object : Logger {
        private val collector = configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY] ?: MessageCollector.NONE
        override fun warning(message: String) = collector.report(CompilerMessageSeverity.STRONG_WARNING, message)
        override fun error(message: String) = collector.report(CompilerMessageSeverity.ERROR, message)
        override fun log(message: String) = collector.report(CompilerMessageSeverity.LOGGING, message)
        override fun fatal(message: String): Nothing {
            collector.report(CompilerMessageSeverity.ERROR, message)
            (collector as? GroupingMessageCollector)?.flush()
            kotlin.error(message)
        }
    }

    return ModuleLoader(
        libraries,
        ModuleLoader.jsMetadataFactories.DefaultDeserializedDescriptorFactory,
        configuration,
        LockBasedStorageManager("JS Repl"),
        logger
    ).dependencyDescriptors.values
}

fun createCompileResult(code: String) = createCompileResult(LineId(0, 0, 0), code)

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
    // TODO: this should be taken from CompilerConfiguration
    private val commonPath = "libraries/stdlib/js-ir/build/classes/kotlin/js/main/"
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
        for (entry in nameTables.mappedNames.orEmpty()) {
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

