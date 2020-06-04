/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisContext
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.extensions.ExtraImportsProviderExtension
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.*
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.utils.replaceImportingScopes
import org.jetbrains.kotlin.scripting.definitions.ScriptPriorities
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvm.util.CompiledHistoryItem
import kotlin.script.experimental.jvm.util.CompiledHistoryList
import kotlin.script.experimental.jvm.util.SnippetsHistory

open class ReplCodeAnalyzerBase(
    environment: KotlinCoreEnvironment,
    val trace: BindingTraceContext = NoScopeRecordCliBindingTrace(),
    implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter? = null
) {
    protected val scriptDeclarationFactory: ScriptMutableDeclarationProviderFactory

    protected val container: ComponentProvider
    protected val topDownAnalysisContext: TopDownAnalysisContext
    private val topDownAnalyzer: LazyTopDownAnalyzer
    protected val resolveSession: ResolveSession
    protected val replState = ResettableAnalyzerState()

    val module: ModuleDescriptorImpl

    init {
        // Module source scope is empty because all binary classes are in the dependency module, and all source classes are guaranteed
        // to be found via ResolveSession. The latter is true as long as light classes are not needed in REPL (which is currently true
        // because no symbol declared in the REPL session can be used from Java)
        container = TopDownAnalyzerFacadeForJVM.createContainer(
            environment.project,
            emptyList(),
            trace,
            environment.configuration,
            environment::createPackagePartProvider,
            { _, _ -> ScriptMutableDeclarationProviderFactory() },
            implicitsResolutionFilter = implicitsResolutionFilter
        )

        this.module = container.get()
        this.scriptDeclarationFactory = container.get()
        this.resolveSession = container.get()
        this.topDownAnalysisContext = TopDownAnalysisContext(
            TopDownAnalysisMode.TopLevelDeclarations, DataFlowInfoFactory.EMPTY, resolveSession.declarationScopeProvider
        )
        this.topDownAnalyzer = container.get()
    }

    interface ReplLineAnalysisResult {
        val scriptDescriptor: ClassDescriptorWithResolutionScopes?
        val diagnostics: Diagnostics

        data class Successful(
            override val scriptDescriptor: ClassDescriptorWithResolutionScopes,
            override val diagnostics: Diagnostics
        ) :
            ReplLineAnalysisResult

        data class WithErrors(override val diagnostics: Diagnostics) :
            ReplLineAnalysisResult {
            override val scriptDescriptor: ClassDescriptorWithResolutionScopes? get() = null
        }
    }

    fun resetToLine(lineId: ILineId): List<SourceCodeByReplLine> = replState.resetToLine(lineId)

    fun reset(): List<SourceCodeByReplLine> = replState.reset()

    fun analyzeReplLine(psiFile: KtFile, codeLine: ReplCodeLine): ReplLineAnalysisResult {
        topDownAnalysisContext.scripts.clear()
        trace.clearDiagnostics()

        psiFile.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, codeLine.no)

        return doAnalyze(psiFile, emptyList(), codeLine.toSourceCode())
    }

    fun analyzeReplLineWithImportedScripts(
        psiFile: KtFile,
        importedScripts: List<KtFile>,
        codeLine: SourceCode,
        priority: Int
    ): ReplLineAnalysisResult {
        topDownAnalysisContext.scripts.clear()
        trace.clearDiagnostics()

        psiFile.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, priority)

        return doAnalyze(psiFile, importedScripts, codeLine.addNo(priority))
    }

    protected fun runAnalyzer(linePsi: KtFile, importedScripts: List<KtFile>): TopDownAnalysisContext {
        return topDownAnalyzer.analyzeDeclarations(topDownAnalysisContext.topDownAnalysisMode, listOf(linePsi) + importedScripts)
    }

    private fun doAnalyze(linePsi: KtFile, importedScripts: List<KtFile>, codeLine: SourceCodeByReplLine): ReplLineAnalysisResult {
        scriptDeclarationFactory.setDelegateFactory(
            FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(linePsi) + importedScripts)
        )
        replState.submitLine(linePsi)

        val context = runAnalyzer(linePsi, importedScripts)

        val diagnostics = trace.bindingContext.diagnostics
        val hasErrors = diagnostics.any { it.severity == Severity.ERROR }
        return if (hasErrors) {
            replState.lineFailure(linePsi)
            ReplLineAnalysisResult.WithErrors(
                diagnostics
            )
        } else {
            val scriptDescriptor = context.scripts[linePsi.script]!!
            replState.lineSuccess(linePsi, codeLine, scriptDescriptor)
            ReplLineAnalysisResult.Successful(
                scriptDescriptor,
                diagnostics
            )
        }
    }

    protected class ScriptMutableDeclarationProviderFactory : DeclarationProviderFactory {
        private lateinit var delegateFactory: DeclarationProviderFactory
        private lateinit var rootPackageProvider: AdaptablePackageMemberDeclarationProvider

        fun setDelegateFactory(delegateFactory: DeclarationProviderFactory) {
            this.delegateFactory = delegateFactory

            val provider = delegateFactory.getPackageMemberDeclarationProvider(FqName.ROOT)!!
            try {
                rootPackageProvider.addDelegateProvider(provider)
            } catch (e: UninitializedPropertyAccessException) {
                rootPackageProvider =
                    AdaptablePackageMemberDeclarationProvider(
                        provider
                    )
            }
        }

        override fun getClassMemberDeclarationProvider(classLikeInfo: KtClassLikeInfo): ClassMemberDeclarationProvider {
            return delegateFactory.getClassMemberDeclarationProvider(classLikeInfo)
        }

        override fun getPackageMemberDeclarationProvider(packageFqName: FqName): PackageMemberDeclarationProvider? {
            if (packageFqName.isRoot) {
                return rootPackageProvider
            }

            return delegateFactory.getPackageMemberDeclarationProvider(packageFqName)
        }

        override fun diagnoseMissingPackageFragment(fqName: FqName, file: KtFile?) {
            delegateFactory.diagnoseMissingPackageFragment(fqName, file)
        }

        class AdaptablePackageMemberDeclarationProvider(
            private var delegateProvider: PackageMemberDeclarationProvider
        ) : DelegatePackageMemberDeclarationProvider(delegateProvider) {
            fun addDelegateProvider(provider: PackageMemberDeclarationProvider) {
                val combinedDelegateProvider = delegateProvider as? CombinedPackageMemberDeclarationProvider
                val providers =
                    if (combinedDelegateProvider != null) listOf(provider) + combinedDelegateProvider.providers
                    else listOf(provider, delegateProvider)
                delegateProvider = CombinedPackageMemberDeclarationProvider(providers)

                delegate = delegateProvider
            }
        }
    }

    data class CompiledCode(val className: String, val source: SourceCodeByReplLine)

    // TODO: merge with org.jetbrains.kotlin.resolve.repl.ReplState when switching to new REPL infrastructure everywhere
    // TODO: review its place in the extracted state infrastructure (now the analyzer itself is a part of the state)
    class ResettableAnalyzerState {
        private val successfulLines = ResettableSnippetsHistory<LineInfo.SuccessfulLine>()
        private val submittedLines = hashMapOf<KtFile, LineInfo>()

        fun resetToLine(lineId: ILineId): List<SourceCodeByReplLine> {
            val removed = successfulLines.resetToLine(lineId)
            removed.forEach { submittedLines.remove(it.second.linePsi) }
            return removed.map { it.first }
        }

        fun reset(): List<SourceCodeByReplLine> {
            submittedLines.clear()
            return successfulLines.reset().map { it.first }
        }

        fun submitLine(ktFile: KtFile) {
            val line =
                LineInfo.SubmittedLine(
                    ktFile,
                    successfulLines.lastValue()
                )
            submittedLines[ktFile] = line
            ktFile.fileScopesCustomizer = object : FileScopesCustomizer {
                override fun createFileScopes(fileScopeFactory: FileScopeFactory): FileScopes {
                    return lineInfo(ktFile)?.let { computeFileScopes(it, fileScopeFactory) } ?: fileScopeFactory.createScopesForFile(ktFile)
                }
            }
        }

        fun lineSuccess(ktFile: KtFile, codeLine: SourceCodeByReplLine, scriptDescriptor: ClassDescriptorWithResolutionScopes) {
            val successfulLine =
                LineInfo.SuccessfulLine(
                    ktFile,
                    successfulLines.lastValue(),
                    scriptDescriptor
                )
            submittedLines[ktFile] = successfulLine
            successfulLines.add(
                CompiledCode(
                    ktFile.name,
                    codeLine
                ), successfulLine
            )
        }

        fun lineFailure(ktFile: KtFile) {
            submittedLines[ktFile] =
                LineInfo.FailedLine(
                    ktFile,
                    successfulLines.lastValue()
                )
        }

        private fun lineInfo(ktFile: KtFile) = submittedLines[ktFile]

        // use sealed?
        private sealed class LineInfo {
            abstract val linePsi: KtFile
            abstract val parentLine: SuccessfulLine?

            class SubmittedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?) : LineInfo()
            class SuccessfulLine(
                override val linePsi: KtFile,
                override val parentLine: SuccessfulLine?,
                val lineDescriptor: ClassDescriptorWithResolutionScopes
            ) : LineInfo()

            class FailedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?) : LineInfo()
        }

        private fun computeFileScopes(lineInfo: LineInfo, fileScopeFactory: FileScopeFactory): FileScopes? {
            val linePsi = lineInfo.linePsi
            val hasImports = linePsi.importDirectives.isNotEmpty() ||
                    ExtraImportsProviderExtension.getInstance(linePsi.project).getExtraImports(linePsi).isNotEmpty()

            // create scope that wraps previous line lexical scope and adds imports from this line
            val lexicalScopeAfterLastLine = lineInfo.parentLine?.lineDescriptor?.scopeForInitializerResolution ?: return null
            val lastLineImports = lexicalScopeAfterLastLine.parentsWithSelf.first { it is ImportingScope } as ImportingScope
            val scopesForThisLine = fileScopeFactory.createScopesForFile(linePsi, lastLineImports)
            val combinedLexicalScopes = if (hasImports)
                lexicalScopeAfterLastLine.replaceImportingScopes(scopesForThisLine.importingScope)
            else
                lexicalScopeAfterLastLine

            return FileScopes(combinedLexicalScopes, scopesForThisLine.importingScope, scopesForThisLine.importForceResolver)
        }
    }
}

fun ReplCodeLine.toSourceCode() = SourceCodeByReplLine(code, no)
internal fun SourceCode.addNo(no: Int) = SourceCodeByReplLine(text, no, name, locationId)

data class SourceCodeByReplLine(
    override val text: String,
    val no: Int,
    override val name: String? = null,
    override val locationId: String? = null
) : SourceCode

private typealias ReplSourceHistoryList<ResultT> = List<CompiledHistoryItem<SourceCodeByReplLine, ResultT>>

@Deprecated("This functionality is left for backwards compatibility only", ReplaceWith("SnippetsHistory"))
private class ResettableSnippetsHistory<ResultT>(startingHistory: CompiledHistoryList<ReplCodeAnalyzerBase.CompiledCode, ResultT> = emptyList()) :
    SnippetsHistory<ReplCodeAnalyzerBase.CompiledCode, ResultT>(startingHistory) {

    fun resetToLine(line: ILineId): ReplSourceHistoryList<ResultT> {
        val removed = arrayListOf<Pair<SourceCodeByReplLine, ResultT>>()
        while ((history.lastOrNull()?.first?.source?.no ?: -1) > line.no) {
            removed.add(history.removeAt(history.size - 1).let { Pair(it.first.source, it.second) })
        }
        return removed.reversed()
    }

    fun reset(): ReplSourceHistoryList<ResultT> {
        val removed = history.map { Pair(it.first.source, it.second) }
        history.clear()
        return removed
    }
}
