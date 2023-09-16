/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.createLibraryListForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.*
import org.jetbrains.kotlin.cli.jvm.compiler.toAbstractProjectEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.FirReplRegistrar
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.util.PropertiesCollection

class K2JvmReplCompiler {

    private val replState = ReplStateImpl(FqName("myRepl"))

    suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<List<KJvmCompiledScript>> {
        val context = provideCompilationContext(configuration)

        val result = snippets.flatMapSuccess { snippet ->
            withMessageCollector(snippet) { messageCollector ->
                compileImpl(snippet, messageCollector, configuration, context)
            }
        }

        //TODO: remove
        return result
    }

    private suspend fun compileImpl(
        snippet: SourceCode,
        messageCollector: ScriptDiagnosticsMessageCollector,
        configuration: ScriptCompilationConfiguration,
        context: SharedScriptCompilationContext,
    ): ResultWithDiagnostics<List<KJvmCompiledScript>> {
        val initialConfiguration = configuration.refineBeforeParsing(snippet).valueOr { return it }

        val errorHolder = object : MessageCollectorBasedReporter {
            override val messageCollector = messageCollector
        }
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val ktFile = prepareForAnalyze(snippet, errorHolder, context, failOnSyntaxErrors = true).valueOr { return it }
        collectRefinedSourcesAndUpdateEnvironment(
            context,
            ktFile,
            initialConfiguration,
            messageCollector
        )

        val sourceFiles = listOf(ktFile)

        // TODO: is it needed?
        checkKotlinPackageUsageForPsi(context.environment.configuration, sourceFiles, messageCollector)

        if (messageCollector.hasErrors()) return failure(messageCollector)

        val lineId = LineId(replState.nextSnippetId(), 0, snippet.hashCode())

        val rootModuleName = "snippet_${lineId.no}"
        val kotlinCompilerConfiguration = context.environment.configuration
        val renderDiagnosticName = kotlinCompilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)

        val projectEnvironment = context.environment.toAbstractProjectEnvironment()
        val compilerEnvironment = ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter)

        val librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
        val libraryList = createLibraryListForJvm(
            rootModuleName,
            kotlinCompilerConfiguration,
            friendPaths = emptyList()
        )

        val extensionRegistrars = (projectEnvironment as? VfsBasedProjectEnvironment)
            ?.let { FirExtensionRegistrar.getInstances(it.project) }
            .orEmpty() + FirReplRegistrar(replState)

        val targetId = TargetId(
            kotlinCompilerConfiguration[CommonConfigurationKeys.MODULE_NAME] ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME,
            "java-production"
        )

        val sources = sourceFiles.map { KtPsiSourceFile(it) }

        val compilerInput = ModuleCompilerInput(
            targetId,
            GroupedKtSources(platformSources = sources, commonSources = emptySet(), sourcesByModuleName = emptyMap()),
            CommonPlatforms.defaultCommonPlatform,
            JvmPlatforms.unspecifiedJvmPlatform,
            kotlinCompilerConfiguration
        )

        val session = prepareJvmSessions(
            sourceFiles, kotlinCompilerConfiguration, projectEnvironment, Name.special("<$rootModuleName>"), extensionRegistrars = extensionRegistrars,
            librariesScope, libraryList, isCommonSourceForPsi, fileBelongsToModuleForPsi,
            createProviderAndScopeForIncrementalCompilation = { files ->
                // TODO: something else?
                null
            }
        ).single().session

        val rawFir = session.buildFirFromKtFiles(sourceFiles)

        val compiledScripts = mutableListOf<KJvmCompiledScript>()

        for (firFile in rawFir) {
            val (scopeSession, fir) = session.runResolution(listOf(firFile))
            // checkers
            session.runCheckers(scopeSession, fir, diagnosticsReporter)

            // Collect variables from snippets here


            val analysisResults = FirResult(listOf(ModuleCompilerAnalyzedOutput(session, scopeSession, fir)))

            if (diagnosticsReporter.hasErrors) {
                diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
                return failure(messageCollector)
            }

            val irInput = convertAnalyzedFirToIr(compilerInput, analysisResults, compilerEnvironment)

            val codegenOutput = generateCodeFromIr(irInput, compilerEnvironment, null)

            diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)

            if (diagnosticsReporter.hasErrors) {
                return failure(messageCollector)
            }

            val compiledScript = makeCompiledScript(
                codegenOutput.generationState,
                snippet,
                ktFile,
                emptyList(),
                { initialConfiguration } // TODO: provide refined configuration?
            )
            when (compiledScript) {
                is ResultWithDiagnostics.Success<KJvmCompiledScript> -> compiledScripts.add(compiledScript.value)
                is ResultWithDiagnostics.Failure -> return compiledScript
            }
        }

        //TODO: remove
        return ResultWithDiagnostics.Success(compiledScripts, messageCollector.diagnostics)
    }

    protected fun prepareForAnalyze(
        snippet: SourceCode,
        errorHolder: MessageCollectorBasedReporter,
        context: SharedScriptCompilationContext,
        failOnSyntaxErrors: Boolean
    ): ResultWithDiagnostics<KtFile> =
        withMessageCollector(
            snippet,
            errorHolder.messageCollector
        ) { messageCollector ->
            setIdeaIoUseFallback()

            val snippetKtFile =
                getScriptKtFile(
                    snippet,
                    context.baseScriptCompilationConfiguration,
                    context.environment.project,
                    messageCollector
                )
                    .valueOr { return it }

            val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(snippetKtFile, errorHolder)
            if (syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof) {
                messageCollector.report(ScriptDiagnostic(ScriptDiagnostic.incompleteCode, "Incomplete code"))
            }
            if (failOnSyntaxErrors && syntaxErrorReport.isHasErrors) return failure(messageCollector)

            return snippetKtFile.asSuccess()
        }

    private fun provideCompilationContext(configuration: ScriptCompilationConfiguration): SharedScriptCompilationContext {
        // It's not clear if we wish to recreate context every time or reuse the existing one
        TODO()
    }
}

interface ReplVariable {
    val symbol: FirVariableSymbol<*>

    val enhancedProperty: FirVariable?
    val enhancedType: String?
    val value: Any?
    val hasValue: Boolean

    fun setValue(value: Any?)
}

interface ReplState {
    val name: FqName

    val variables: MutableMap<Name, ReplVariable>
    fun nextSnippetId(): Int

    fun addDeclaration(symbol: FirVariableSymbol<*>)

    //fun setValueForDeclaration(snippetName: Name, symbol: FirVariableSymbol<*>, value: Any?)

    fun addPackage(packageName: FqName)

    fun processPackages(processor: (packageName: FqName) -> Boolean)

    fun findVariable(name: Name): ReplVariable?
}

class ReplVariableImpl(
    override val symbol: FirVariableSymbol<*>,
): ReplVariable {

    override var enhancedProperty: FirVariable? = null
    override var enhancedType: String? = null

    private var _value: Any? = null

    override val value: Any? get() = _value

    private var _isInitialized = false

    override val hasValue: Boolean
        get() = _isInitialized

    override fun setValue(value: Any?) {
        _value = value
        _isInitialized = true
    }
}

class ReplStateImpl(
    override val name: FqName
): ReplState {
    private val _variables: MutableMap<Name, ReplVariable> = HashMap()
    //private val variableByFullName: MutableMap<String, ReplVariable> = mutableMapOf()
    private var _nextSnippetId = 1
    private val packages = ArrayList<FqName>()

    override val variables: MutableMap<Name, ReplVariable> = _variables
    override fun nextSnippetId(): Int {
        return (_nextSnippetId++)
    }

//    private fun getFullName(snippetName: Name, name: Name): String {
//        return "snippet_${snippetName}_${name.asString()}"
//    }

    override fun addDeclaration(symbol: FirVariableSymbol<*>) {
        //val fullName = getFullName(snippetName, symbol.name)
        val variable = ReplVariableImpl(symbol)
        //variableByFullName[fullName] = variable
        _variables[symbol.name] = variable
    }

//    override fun setValueForDeclaration(snippetName: Name, symbol: FirVariableSymbol<*>, value: Any?) {
//        val fullName = getFullName(snippetName, symbol.name)
//        val variable = variableByFullName[fullName] ?: return
//        variable.setValue(value)
//    }

    override fun addPackage(packageName: FqName) {
        packages.add(packageName)
    }

    override fun processPackages(processor: (packageName: FqName) -> Boolean) {
        for (packageName in packages.asReversed()) {
            if (!processor(packageName)) return
        }
    }

    override fun findVariable(name: Name): ReplVariable? {
        // TODO: optimize search
        return variables[name]
    }
}

val ScriptCompilationConfigurationKeys.replState by PropertiesCollection.key<ReplState?>(null)
