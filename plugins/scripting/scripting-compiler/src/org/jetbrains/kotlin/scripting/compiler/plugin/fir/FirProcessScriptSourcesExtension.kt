/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.fir

import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.findFileByPath
import org.jetbrains.kotlin.cli.jvm.compiler.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.registerInProject
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirProcessSourcesBeforeCompilingExtension
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingK2CompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.*
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptingModuleDataProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.collectAndResolveScriptAnnotationsViaFir
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.convertToFirViaLightTree
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.refineAllForK2
import org.jetbrains.kotlin.scripting.compiler.plugin.toCompilerMessageSeverity
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.resolve.toSourceCode
import org.jetbrains.kotlin.utils.topologicalSort
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class FirProcessScriptSourcesExtension : FirProcessSourcesBeforeCompilingExtension() {
    override fun isApplicable(configuration: CompilerConfiguration): Boolean =
        configuration.getBoolean(ScriptingConfigurationKeys.DISABLE_SCRIPTING_PLUGIN_OPTION) == false

    @OptIn(SessionConfiguration::class)
    override fun doProcessSources(
        environment: Any,
        configuration: CompilerConfiguration,
        sources: Iterable<KtSourceFile>
    ): Iterable<KtSourceFile> {
        environment as VfsBasedProjectEnvironment
        val hostConfiguration = ensureUpdatedHostConfiguration(configuration)

        fun SourceCode.collectImports(): List<SourceCode>? {
            val refinedScriptCompilationConfiguration =
                hostConfiguration.getOrStoreRefinedCompilationConfiguration(this) { script, scriptCompilationConfiguration ->
                    scriptCompilationConfiguration.refineAllForK2(script, hostConfiguration) { script, scriptCompilationConfiguration ->
                        collectAndResolveScriptAnnotationsViaFir(
                            script, scriptCompilationConfiguration, hostConfiguration,
                            { _, scriptCompilationConfiguration ->
                                getOrCreateSessionForAnnotationResolution(
                                    scriptCompilationConfiguration[ScriptCompilationConfiguration.hostConfiguration] ?: hostConfiguration,
                                    configuration,
                                    environment
                                )
                            },
                            SourceCode::convertToFirViaLightTree
                        )
                    }
                }.onFailure {
                    for (report in it.reports) {
                        configuration.report(report.severity.toCompilerMessageSeverity(), report.render(withSeverity = false))
                    }
                }.valueOrNull()
            return refinedScriptCompilationConfiguration?.get(ScriptCompilationConfiguration.importScripts)?.takeIf { it.isNotEmpty() }
        }

        fun toSourceFile(import: SourceCode): KtVirtualFileSourceFile? =
            if (import is FileBasedScriptSource)
                environment.findFileByPath(import.file.path)?.let { virtualFile -> KtVirtualFileSourceFile(virtualFile) } ?: run {
                    configuration.report(
                        CompilerMessageSeverity.ERROR,
                        "Unable to find imported script ${import.file}"
                    )
                    null
                }
            else null  // TODO: support non-file sources


        val sourcesToFiles = sources.associateByTo(mutableMapOf(), KtSourceFile::toSourceCode)
        val remainingSources = ArrayList<SourceCode>().also { it.addAll(sourcesToFiles.keys) }
        val knownSources = sourcesToFiles.keys.mapTo(mutableSetOf()) { it.locationId }
        val sourceDependencies = mutableMapOf<SourceCode, List<SourceCode>>()

        while (remainingSources.isNotEmpty()) {
            val sourceCode = remainingSources.pop()
            sourceCode.collectImports()?.let { imports ->
                imports.filter { knownSources.add(it.locationId) }.forEach { newImport ->
                    remainingSources.add(newImport)
                    toSourceFile(newImport)?.let {
                        sourcesToFiles[newImport] = it
                    }
                }
                sourceDependencies[sourceCode] = imports
            }
        }

        class CycleDetected(val node: SourceCode) : Throwable()
        return try {
            topologicalSort(
                sourcesToFiles.keys, reportCycle = { throw CycleDetected(it) }
            ) {
                sourceDependencies[this] ?: emptyList()
            }.reversed().mapNotNull { sourcesToFiles[it] }
        } catch (e: CycleDetected) {
            configuration.report(
                CompilerMessageSeverity.ERROR,
                "Unable to handle recursive script dependencies, cycle detected on file ${e.node.name ?: e.node.locationId}",
            )
            sourcesToFiles.values
        }
    }

    private fun ensureUpdatedHostConfiguration(configuration: CompilerConfiguration): ScriptingHostConfiguration {
        val providedHostConfiguration = configuration.scriptingHostConfiguration as? ScriptingHostConfiguration
        val definitionProvider =
            providedHostConfiguration?.get(ScriptingHostConfiguration.scriptCompilationConfigurationProvider)
                ?: ScriptCompilationConfigurationProviderOverDefinitionProvider(
                    CliScriptDefinitionProvider().also {
                        it.setScriptDefinitionsSources(configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES))
                        it.setScriptDefinitions(configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS))
                    }
                )
        val refinedCache =
            providedHostConfiguration?.get(ScriptingHostConfiguration.scriptRefinedCompilationConfigurationsCache)
                ?: ScriptRefinedCompilationConfigurationCacheImpl()
        val updatedHostConfiguration = (providedHostConfiguration ?: defaultJvmScriptingHostConfiguration).with {
            scriptCompilationConfigurationProvider.replaceOnlyDefault(definitionProvider)
            scriptRefinedCompilationConfigurationsCache.replaceOnlyDefault(refinedCache)
        }
        configuration.scriptingHostConfiguration = updatedHostConfiguration
        return updatedHostConfiguration
    }

    @SessionConfiguration
    @Synchronized
    private fun getOrCreateSessionForAnnotationResolution(
        hostConfiguration: ScriptingHostConfiguration,
        configuration: CompilerConfiguration,
        projectEnvironment: VfsBasedProjectEnvironment,
    ): FirSession =
        dummySessionForAnnotationResolution ?: run {

            val extensionStorage = CompilerPluginRegistrar.ExtensionStorage()
            with(ScriptingK2CompilerPluginRegistrar()) { extensionStorage.registerExtensions(configuration) }
            extensionStorage.registerInProject(projectEnvironment.project) {
                "Error on plugin registration: ${it.javaClass.name}"
            }

            val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)

            val classpath = hostConfiguration[ScriptingHostConfiguration.configurationDependencies].orEmpty().flatMap {
                (it as? JvmDependency)?.classpath ?: emptyList()
            }

            val moduleDataProvider = ScriptingModuleDataProvider("dummy", classpath.map(File::toPath))

            val sessionFactoryContext = FirJvmSessionFactory.Context(
                configuration = configuration,
                projectEnvironment = projectEnvironment,
                librariesScope = PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(projectEnvironment.project)),
            )
            val sharedLibrarySession = FirJvmSessionFactory.createSharedLibrarySession(
                mainModuleName = Name.special("<dummy>"),
                extensionRegistrars = extensionRegistrars,
                languageVersionSettings = configuration.languageVersionSettings,
                context = sessionFactoryContext,
            )

            FirJvmSessionFactory.createLibrarySession(
                sharedLibrarySession,
                moduleDataProvider = moduleDataProvider,
                extensionRegistrars = extensionRegistrars,
                languageVersionSettings = configuration.languageVersionSettings,
                context = sessionFactoryContext,
            )

            FirJvmSessionFactory.createSourceSession(
                moduleData = moduleDataProvider.addNewScriptModuleData(Name.special("<raw-script>")),
                javaSourcesScope = AbstractProjectFileSearchScope.EMPTY,
                createIncrementalCompilationSymbolProviders = { null },
                extensionRegistrars = extensionRegistrars,
                configuration = configuration,
                context = sessionFactoryContext,
                needRegisterJavaElementFinder = true,
                isForLeafHmppModule = false,
                init = {},
            ).apply {
                register(
                    FirScriptCompilationComponent::class,
                    FirScriptCompilationComponent(hostConfiguration, { _, _ -> this })
                )
                dummySessionForAnnotationResolution = this
            }
        }

    private var dummySessionForAnnotationResolution: FirSession? = null
}
