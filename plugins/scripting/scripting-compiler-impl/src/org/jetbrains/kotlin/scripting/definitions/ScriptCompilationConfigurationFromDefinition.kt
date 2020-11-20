/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import java.io.File
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyExpectedLocations
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.util.PropertiesCollection

class ScriptCompilationConfigurationFromDefinition(
    val hostConfiguration: ScriptingHostConfiguration,
    val scriptDefinition: KotlinScriptDefinition
) : ScriptCompilationConfiguration(
    {
        hostConfiguration(hostConfiguration)
        displayName(scriptDefinition.name)
        fileExtension(scriptDefinition.fileExtension)
        baseClass(KotlinType(scriptDefinition.template))
        implicitReceivers.putIfAny(scriptDefinition.implicitReceivers.map(::KotlinType))
        providedProperties.putIfAny(scriptDefinition.providedProperties.map { it.first to KotlinType(it.second) })
        annotationsForSamWithReceivers.put(scriptDefinition.annotationsForSamWithReceivers.map(::KotlinType))
        platform(scriptDefinition.platform)
        @Suppress("DEPRECATION")
        compilerOptions.putIfAny(scriptDefinition.additionalCompilerArguments)
        ide {
            acceptedLocations.put(scriptDefinition.scriptExpectedLocations.mapLegacyExpectedLocations())
        }
        if (scriptDefinition.dependencyResolver != DependenciesResolver.NoDependencies) {
            refineConfiguration {
                onAnnotations(scriptDefinition.acceptedAnnotations.map(::KotlinType)) { context ->

                    val resolveResult: DependenciesResolver.ResolveResult = scriptDefinition.dependencyResolver.resolve(
                        ScriptContentsFromRefinementContext(context),
                        context.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]?.let {
                            it[ScriptingHostConfiguration.getEnvironment]?.invoke()
                        }.orEmpty()
                    )

                    val reports = resolveResult.reports.map {
                        ScriptDiagnostic(ScriptDiagnostic.unspecifiedError, it.message, mapLegacyDiagnosticSeverity(it.severity))
                    }
                    val resolvedDeps = (resolveResult as? DependenciesResolver.ResolveResult.Success)?.dependencies

                    if (resolvedDeps == null) ResultWithDiagnostics.Failure(reports)
                    else ScriptCompilationConfiguration(context.compilationConfiguration) {
                        if (resolvedDeps.classpath.isNotEmpty()) {
                            dependencies.append(JvmDependency(resolvedDeps.classpath))
                        }
                        defaultImports.append(resolvedDeps.imports)
                        importScripts.append(resolvedDeps.scripts.map { FileScriptSource(it) })
                        jvm {
                            jdkHome.putIfNotNull(resolvedDeps.javaHome) // TODO: check if it is correct to supply javaHome as jdkHome
                        }
                        if (resolvedDeps.sources.isNotEmpty()) {
                            ide {
                                dependenciesSources.append(JvmDependency(resolvedDeps.sources))
                            }
                        }
                    }.asSuccess(reports)
                }
            }
        }
    }
)

private class ScriptContentsFromRefinementContext(val context: ScriptConfigurationRefinementContext) : ScriptContents {
    override val file: File?
        get() = (context.script as? FileBasedScriptSource)?.file
    override val annotations: Iterable<Annotation>
        get() = context.collectedData?.get(ScriptCollectedData.foundAnnotations) ?: emptyList()
    override val text: CharSequence?
        get() = context.script.text
}

val ScriptCompilationConfigurationKeys.annotationsForSamWithReceivers by PropertiesCollection.key<List<KotlinType>>()

val ScriptCompilationConfigurationKeys.platform by PropertiesCollection.key<String>()

val ScriptingHostConfigurationKeys.getEnvironment by PropertiesCollection.key<() -> Environment?>()