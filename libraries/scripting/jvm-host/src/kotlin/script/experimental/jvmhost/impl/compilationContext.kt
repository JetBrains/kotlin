/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.dependencies.ScriptsCompilationDependencies
import org.jetbrains.kotlin.scripting.dependencies.collectScriptsCompilationDependencies
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.KotlinJars
import kotlin.script.experimental.jvm.withUpdatedClasspath

internal class SharedScriptCompilationContext(
    val disposable: Disposable,
    val baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    val environment: KotlinCoreEnvironment,
    val ignoredOptionsReportingState: IgnoredOptionsReportingState,
    val scriptCompilationState: BridgeScriptDefinitionDynamicState
)

internal fun createSharedCompilationContext(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    messageCollector: ScriptDiagnosticsMessageCollector,
    disposable: Disposable
): SharedScriptCompilationContext {
    val ignoredOptionsReportingState = IgnoredOptionsReportingState()

    val scriptCompilationState = BridgeScriptDefinitionDynamicState()

    val (initialScriptCompilationConfiguration, kotlinCompilerConfiguration) =
        createInitialConfigurations(
            scriptCompilationConfiguration, hostConfiguration, scriptCompilationState, messageCollector, ignoredOptionsReportingState
        )
    val environment =
        if (System.getProperty("idea.is.unit.test") == "true") KotlinCoreEnvironment.createForTests(
            disposable, kotlinCompilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        else KotlinCoreEnvironment.createForProduction(
            disposable, kotlinCompilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

    return SharedScriptCompilationContext(
        disposable, initialScriptCompilationConfiguration, environment, ignoredOptionsReportingState, scriptCompilationState
    )
}

internal fun createInitialConfigurations(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    scriptCompilationState: BridgeScriptDefinitionDynamicState,
    messageCollector: ScriptDiagnosticsMessageCollector,
    ignoredOptionsReportingState: IgnoredOptionsReportingState
): Pair<ScriptCompilationConfiguration, CompilerConfiguration> {
    val kotlinCompilerConfiguration =
        createInitialCompilerConfiguration(
            scriptCompilationConfiguration, hostConfiguration, messageCollector, ignoredOptionsReportingState
        )

    val initialScriptCompilationConfiguration =
        scriptCompilationConfiguration.withUpdatesFromCompilerConfiguration(kotlinCompilerConfiguration)

    kotlinCompilerConfiguration.add(
        ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
        BridgeScriptDefinition(
            scriptCompilationConfiguration,
            hostConfiguration,
            scriptCompilationState
        )
    )

    return Pair(initialScriptCompilationConfiguration, kotlinCompilerConfiguration)
}

private fun ScriptCompilationConfiguration.withUpdatesFromCompilerConfiguration(kotlinCompilerConfiguration: CompilerConfiguration) =
    withUpdatedClasspath(kotlinCompilerConfiguration.jvmClasspathRoots)

private fun createInitialCompilerConfiguration(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    messageCollector: MessageCollector,
    reportingState: IgnoredOptionsReportingState
): CompilerConfiguration {

    val baseArguments = K2JVMCompilerArguments()
    parseCommandLineArguments(
        scriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions] ?: emptyList(),
        baseArguments
    )

    reportArgumentsIgnoredGenerally(baseArguments, messageCollector, reportingState)
    reportingState.currentArguments = baseArguments

    return CompilerConfiguration().apply {
        put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        setupCommonArguments(baseArguments)

        setupJvmSpecificArguments(baseArguments)

        // Default value differs from the argument's default (see #KT-29405 and #KT-29319)
        put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)

        val jdkHomeFromConfigurations = scriptCompilationConfiguration.getNoDefault(ScriptCompilationConfiguration.jvm.jdkHome)
            ?: hostConfiguration[ScriptingHostConfiguration.jvm.jdkHome]
        if (jdkHomeFromConfigurations != null) {
            messageCollector.report(CompilerMessageSeverity.LOGGING, "Using JDK home directory $jdkHomeFromConfigurations")
            put(JVMConfigurationKeys.JDK_HOME, jdkHomeFromConfigurations)
        } else {
            configureJdkHome(baseArguments)
        }

        put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

        val isModularJava = isModularJava()

        scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies]?.let { dependencies ->
            addJvmClasspathRoots(
                dependencies.flatMap {
                    (it as JvmDependency).classpath
                }
            )
        }

        add(
            ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS,
            ScriptingCompilerConfigurationComponentRegistrar()
        )

        configureExplicitContentRoots(baseArguments)

        if (!baseArguments.noStdlib) {
            addModularRootIfNotNull(isModularJava, "kotlin.stdlib", KotlinJars.stdlib)
            addModularRootIfNotNull(isModularJava, "kotlin.script.runtime", KotlinJars.scriptRuntimeOrNull)
        }
        // see comments about logic in CompilerConfiguration.configureStandardLibs
        if (!baseArguments.noReflect && !baseArguments.noStdlib) {
            addModularRootIfNotNull(isModularJava, "kotlin.reflect", KotlinJars.reflectOrNull)
        }

        put(CommonConfigurationKeys.MODULE_NAME, baseArguments.moduleName ?: "kotlin-script")

        configureAdvancedJvmOptions(baseArguments)
    }
}

internal fun collectRefinedSourcesAndUpdateEnvironment(
    context: SharedScriptCompilationContext,
    mainKtFile: KtFile,
    messageCollector: ScriptDiagnosticsMessageCollector
): Pair<List<KtFile>, List<ScriptsCompilationDependencies.SourceDependencies>> {
    val sourceFiles = arrayListOf(mainKtFile)
    val (classpath, newSources, sourceDependencies) =
        collectScriptsCompilationDependencies(
            context.environment.configuration,
            context.environment.project,
            sourceFiles
        )

    // TODO: consider removing, it is probably redundant: the actual index update is performed with environment.updateClasspath
    context.environment.configuration.addJvmClasspathRoots(classpath)
    context.environment.updateClasspath(classpath.map(::JvmClasspathRoot))

    sourceFiles.addAll(newSources)

    // collectScriptsCompilationDependencies calls resolver for every file, so at this point all updated configurations are collected
    context.environment.configuration.updateWithRefinedConfigurations(
        context.baseScriptCompilationConfiguration, context.scriptCompilationState.configurations,
        messageCollector, context.ignoredOptionsReportingState
    )
    return sourceFiles to sourceDependencies
}

private fun CompilerConfiguration.updateWithRefinedConfigurations(
    initialScriptCompilationConfiguration: ScriptCompilationConfiguration,
    refinedScriptCompilationConfigurations: Map<SourceCode, ScriptCompilationConfiguration>,
    messageCollector: ScriptDiagnosticsMessageCollector,
    reportingState: IgnoredOptionsReportingState
) {
    val updatedCompilerOptions = refinedScriptCompilationConfigurations.flatMap {
        it.value[ScriptCompilationConfiguration.compilerOptions] ?: emptyList()
    }
    if (updatedCompilerOptions.isNotEmpty() &&
        updatedCompilerOptions != initialScriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions]
    ) {

        val updatedArguments = K2JVMCompilerArguments()
        parseCommandLineArguments(updatedCompilerOptions, updatedArguments)

        reportArgumentsIgnoredGenerally(updatedArguments, messageCollector, reportingState)
        reportArgumentsIgnoredFromRefinement(updatedArguments, messageCollector, reportingState)

        setupCommonArguments(updatedArguments)

        setupJvmSpecificArguments(updatedArguments)

        configureAdvancedJvmOptions(updatedArguments)
    }
}