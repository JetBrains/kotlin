/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.javaInterop
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.disableStandardScriptDefinition
import org.jetbrains.kotlin.config.inlineConstTracker
import org.jetbrains.kotlin.config.jvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.*
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with

/**
 * The state shared by the K2 script and REPL compilers. The compiler plugins should be already registered in [compilerConfiguration].
 */
abstract class AbstractScriptingCompilerState<P : ModuleDataProvider> internal constructor(
    val baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    internal val compilerConfiguration: CompilerConfiguration,
    internal val projectEnvironment: VfsBasedProjectEnvironment,
    internal val ignoredOptionsReportingState: IgnoredOptionsReportingState,
    internal val messageCollector: ScriptDiagnosticsMessageCollector,
    hostConfiguration: ScriptingHostConfiguration,
    moduleName: Name,
    createModuleDataProvider: (List<File>) -> P,
) {
    val hostConfiguration: ScriptingHostConfiguration =
        compilerConfiguration.addScriptDefinitionAndConfigureHost(hostConfiguration, baseScriptCompilationConfiguration)
            .also { compilerConfiguration.scriptingHostConfiguration = it }

    internal val moduleDataProvider: P

    internal val extensionRegistrars: List<FirExtensionRegistrar> = compilerConfiguration.getCompilerExtensions(FirExtensionRegistrar)

    internal var sessionFactoryContext: FirJvmSessionFactory.Context =
        createScriptingSessionFactoryContext(compilerConfiguration, projectEnvironment)
        private set

    internal val sharedLibrarySession: FirSession

    init {
        val classpath = baseScriptCompilationConfiguration.baseCompilationClasspath()
        projectEnvironment.updateClasspath(classpath)
        moduleDataProvider = createModuleDataProvider(classpath)
        sharedLibrarySession = createScriptingLibrarySessions(
            moduleName, moduleDataProvider, extensionRegistrars, compilerConfiguration.languageVersionSettings, sessionFactoryContext
        )
    }

    /**
     * Applies the refined compiler options of the [sources] to the [configuration] and updates the session factory context for it.
     */
    internal fun applyRefinedCompilerOptions(
        configuration: CompilerConfiguration,
        sources: List<SourceCode>,
        messageCollector: ScriptDiagnosticsMessageCollector,
        getRefinedConfiguration: (SourceCode) -> ScriptCompilationConfiguration?,
    ) {
        configuration.applyRefinedCompilerOptions(
            baseScriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions].orEmpty(),
            ignoredOptionsReportingState,
            sources,
            messageCollector,
            getRefinedConfiguration,
        )
        sessionFactoryContext = sessionFactoryContext.updatedFor(configuration)
    }

    /**
     * Stripped down session for resolving the annotations of a source before its actual compilation.
     */
    internal val sessionForAnnotationResolution: FirSession by lazy {
        createScriptSourceSession(
            createModuleDataForAnnotationResolution(), extensionRegistrars, compilerConfiguration, sessionFactoryContext, hostConfiguration
        )
    }

    protected abstract fun createModuleDataForAnnotationResolution(): FirModuleData

    val project get() = projectEnvironment.project
}

/**
 * Recreates the context only if the JVM target or the inline constant tracker changed.
 * Keep in sync with what the [FirJvmSessionFactory.Context] constructor reads from the configuration.
 */
private fun FirJvmSessionFactory.Context.updatedFor(configuration: CompilerConfiguration): FirJvmSessionFactory.Context =
    if ((configuration.jvmTarget ?: JvmTarget.DEFAULT) == jvmTarget && configuration.inlineConstTracker === inlineConstTracker) this
    else FirJvmSessionFactory.Context(
        configuration = configuration,
        projectEnvironment = projectEnvironment,
        librariesClasspath = librariesClasspath,
        javaInterop = javaInterop,
    )

private fun CompilerConfiguration.addScriptDefinitionAndConfigureHost(
    hostConfiguration: ScriptingHostConfiguration,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
): ScriptingHostConfiguration {
    add(
        ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
        ScriptDefinition.FromConfigurations(hostConfiguration, scriptCompilationConfiguration, null)
    )
    val scriptDefinitionProvider = CliScriptDefinitionProvider(disableStandardScriptDefinition).also {
        it.setScriptDefinitionsSources(getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES))
        it.setScriptDefinitions(getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS))
    }
    return hostConfiguration.with {
        scriptCompilationConfigurationProvider(ScriptCompilationConfigurationProviderOverDefinitionProvider(scriptDefinitionProvider))
        scriptRefinedCompilationConfigurationsCache(ScriptRefinedCompilationConfigurationCacheImpl())
    }
}

internal fun createScriptingSessionFactoryContext(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
): FirJvmSessionFactory.Context =
    FirJvmSessionFactory.Context(
        configuration = configuration,
        projectEnvironment = projectEnvironment,
        librariesClasspath = JvmClasspath.ProjectLibraries(),
        javaInterop = projectEnvironment.javaInterop(configuration, withJavaSources = false),
    )

/**
 * Creates the shared library session and the library session of the base dependencies; returns the former.
 */
internal fun createScriptingLibrarySessions(
    moduleName: Name,
    moduleDataProvider: ModuleDataProvider,
    extensionRegistrars: List<FirExtensionRegistrar>,
    languageVersionSettings: LanguageVersionSettings,
    context: FirJvmSessionFactory.Context,
): FirSession {
    val sharedLibrarySession = FirJvmSessionFactory.createSharedLibrarySession(
        mainModuleName = moduleName,
        extensionRegistrars = extensionRegistrars,
        languageVersionSettings = languageVersionSettings,
        context = context,
    )
    FirJvmSessionFactory.createLibrarySession(
        sharedLibrarySession,
        moduleDataProvider = moduleDataProvider,
        extensionRegistrars = extensionRegistrars,
        languageVersionSettings = languageVersionSettings,
        context = context,
    )
    return sharedLibrarySession
}
