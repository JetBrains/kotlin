/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.scripting.resolve.ApiChangeDependencyResolverWrapper
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.experimental.location.ScriptExpectedLocations
import kotlin.script.experimental.util.PropertiesCollection
import kotlin.script.templates.AcceptedAnnotations
import kotlin.script.templates.ScriptTemplateDefinition

@Deprecated("Use 'ScriptDefinition' instead", level = DeprecationLevel.WARNING)
class ScriptCompilationConfigurationFromLegacyTemplate(
    val hostConfiguration: ScriptingHostConfiguration,
    val template: KClass<out Any>,
) : ScriptCompilationConfiguration(
    {
        fun <T> takeUnlessError(body: () -> T?): T? = try {
            body()
        } catch (ex: Throwable) {
            log.error("Invalid script template: " + template.qualifiedName, ex)
            null
        }

        fun resolverFromAnnotation(template: KClass<out Any>): DependenciesResolver? {
            val defAnn = takeUnlessError {
                template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>()
            } ?: return null

            return when (val resolver = instantiateResolver(defAnn.resolver)) {
                is AsyncDependenciesResolver -> AsyncDependencyResolverWrapper(resolver)
                is DependenciesResolver -> resolver
                else -> resolver?.let(::ApiChangeDependencyResolverWrapper)
            }
        }

        val dependencyResolver: DependenciesResolver = resolverFromAnnotation(template) ?: DependenciesResolver.NoDependencies

        fun sameSignature(left: KFunction<*>, right: KFunction<*>): Boolean =
            left.name == right.name && left.parameters.size == right.parameters.size && left.parameters.zip(right.parameters).all {
                it.first.kind == KParameter.Kind.INSTANCE || it.first.name == it.second.name
            }

        val acceptedAnnotations = dependencyResolver.unwrap()::class.memberFunctions.asSequence()
            .filter { function -> getResolveFunctions().any { sameSignature(function, it) } }.flatMap { it.annotations }
            .filterIsInstance<AcceptedAnnotations>().flatMap { it.supportedAnnotationClasses.toList() }.distinctBy { it.qualifiedName }
            .map(::KotlinType).toList()

        val scriptExpectedLocations = takeUnlessError {
            template.annotations.firstIsInstanceOrNull<ScriptExpectedLocations>()
        }?.value?.map {
            when (it) {
                ScriptExpectedLocation.SourcesOnly -> ScriptAcceptedLocation.Sources
                ScriptExpectedLocation.TestsOnly -> ScriptAcceptedLocation.Tests
                ScriptExpectedLocation.Libraries -> ScriptAcceptedLocation.Libraries
                ScriptExpectedLocation.Project -> ScriptAcceptedLocation.Project
                ScriptExpectedLocation.Everywhere -> ScriptAcceptedLocation.Everywhere
            }
        } ?: listOf(
            ScriptAcceptedLocation.Sources, ScriptAcceptedLocation.Tests
        )

        val additionalCompilerArguments = takeUnlessError {
            template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateAdditionalCompilerArguments>()?.let {
                it.provider.primaryConstructor?.call(it.arguments.asIterable())
            }
        }?.getAdditionalCompilerArguments(
            hostConfiguration[ScriptingHostConfiguration.getEnvironment]?.invoke().orEmpty()
        )
        platform("JVM")
        hostConfiguration(hostConfiguration)
        displayName(template.simpleName!!)
        baseClass(KotlinType(template))
        @Suppress("DEPRECATION") compilerOptions.putIfAny(additionalCompilerArguments)
        // TODO: remove this exception when gradle switches to the new definitions and sets the property accordingly
        ide {
            acceptedLocations.put(scriptExpectedLocations)
        }
        if (dependencyResolver != DependenciesResolver.NoDependencies) {
            refineConfiguration {
//                beforeCompiling {
//                    refineWithResolver(dependencyResolver, it)
//                }
                onAnnotations(acceptedAnnotations) {
                    refineWithResolver(dependencyResolver, it)
                }
            }
        }
        template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>()?.scriptFilePattern?.let {
            filePathPattern(it)
        }
    })


private fun <T : Any> instantiateResolver(resolverClass: KClass<T>): T? {
    try {
        resolverClass.objectInstance?.let {
            return it
        }
        val constructorWithoutParameters = resolverClass.constructors.find { it.parameters.all { it.isOptional } }
        if (constructorWithoutParameters == null) {
            log.warn("[kts] ${resolverClass.qualifiedName} must have a constructor without required parameters")
            return null
        }
        return constructorWithoutParameters.callBy(emptyMap())
    } catch (ex: ClassCastException) {
        log.warn("[kts] Script def error ${ex.message}")
        return null
    }
}

private fun getResolveFunctions(): List<KFunction<*>> {
    // DependenciesResolver::resolve, ScriptDependenciesResolver::resolve, AsyncDependenciesResolver::resolveAsync
    return AsyncDependenciesResolver::class.memberFunctions.filter { it.name == "resolve" || it.name == "resolveAsync" }.also {
        assert(it.size == 3) {
            AsyncDependenciesResolver::class.memberFunctions.joinToString(prefix = "${AsyncDependenciesResolver::class.qualifiedName} api changed, fix this code") { it.name }
        }
    }
}

internal val log = Logger.getInstance(ScriptCompilationConfigurationFromLegacyTemplate::class.java)

private class ScriptContentsFromRefinementContext(val context: ScriptConfigurationRefinementContext) : ScriptContents {
    override val file: File?
        get() = (context.script as? FileBasedScriptSource)?.file
    override val annotations: Iterable<Annotation>
        get() = context.collectedData?.get(ScriptCollectedData.foundAnnotations) ?: emptyList()
    override val text: CharSequence
        get() = context.script.text
}

private fun refineWithResolver(
    dependencyResolver: DependenciesResolver,
    context: ScriptConfigurationRefinementContext,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val resolveResult: DependenciesResolver.ResolveResult = dependencyResolver.resolve(
        ScriptContentsFromRefinementContext(context), emptyMap()
    )

    val reports = resolveResult.reports.map {
        ScriptDiagnostic(ScriptDiagnostic.unspecifiedError, it.message, mapLegacyDiagnosticSeverity(it.severity))
    }
    val resolvedDeps = (resolveResult as? DependenciesResolver.ResolveResult.Success)?.dependencies

    return if (resolvedDeps == null) ResultWithDiagnostics.Failure(reports)
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

interface DependencyResolverWrapper<T : ScriptDependenciesResolver> {
    val delegate: T
}

fun ScriptDependenciesResolver.unwrap(): ScriptDependenciesResolver {
    return if (this is DependencyResolverWrapper<*>) delegate.unwrap() else this
}


// wraps AsyncDependenciesResolver to provide implementation for synchronous DependenciesResolver::resolve
class AsyncDependencyResolverWrapper(
    override val delegate: AsyncDependenciesResolver,
) : AsyncDependenciesResolver, DependencyResolverWrapper<AsyncDependenciesResolver> {

    override fun resolve(
        scriptContents: ScriptContents, environment: Environment,
    ): DependenciesResolver.ResolveResult =
        @Suppress("DEPRECATION_ERROR")
        internalScriptingRunSuspend { delegate.resolveAsync(scriptContents, environment) }

    override suspend fun resolveAsync(
        scriptContents: ScriptContents, environment: Environment,
    ): DependenciesResolver.ResolveResult = delegate.resolveAsync(scriptContents, environment)
}

val ScriptCompilationConfigurationKeys.annotationsForSamWithReceivers by PropertiesCollection.key<List<KotlinType>>()

val ScriptCompilationConfigurationKeys.platform by PropertiesCollection.key<String>()

val ScriptingHostConfigurationKeys.getEnvironment by PropertiesCollection.key<() -> Environment?>()