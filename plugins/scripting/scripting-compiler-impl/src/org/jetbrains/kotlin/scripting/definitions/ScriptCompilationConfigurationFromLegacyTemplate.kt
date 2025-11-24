/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.impl.fromLegacyTemplate
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.extensions.SamWithReceiverAnnotations
import kotlin.script.templates.AcceptedAnnotations
import kotlin.script.templates.ScriptTemplateAdditionalCompilerArguments
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

        val dependencyResolver: DependenciesResolver = resolverFromAnnotation(template)

        @Suppress("DEPRECATION")
        val scriptExpectedLocations =
            template.annotations.firstIsInstanceOrNull<kotlin.script.experimental.location.ScriptExpectedLocations>()?.value?.map {
                when (it) {
                    kotlin.script.experimental.location.ScriptExpectedLocation.SourcesOnly -> ScriptAcceptedLocation.Sources
                    kotlin.script.experimental.location.ScriptExpectedLocation.TestsOnly -> ScriptAcceptedLocation.Tests
                    kotlin.script.experimental.location.ScriptExpectedLocation.Libraries -> ScriptAcceptedLocation.Libraries
                    kotlin.script.experimental.location.ScriptExpectedLocation.Project -> ScriptAcceptedLocation.Project
                    kotlin.script.experimental.location.ScriptExpectedLocation.Everywhere -> ScriptAcceptedLocation.Everywhere
                }
            } ?: listOf(
                ScriptAcceptedLocation.Sources, ScriptAcceptedLocation.Tests
            )

        val additionalCompilerArguments = takeUnlessError {
            template.annotations.firstIsInstanceOrNull<ScriptTemplateAdditionalCompilerArguments>()?.let {
                it.provider.primaryConstructor?.call(it.arguments.asIterable())
            }
        }?.getAdditionalCompilerArguments(
            hostConfiguration[ScriptingHostConfiguration.getEnvironment]?.invoke().orEmpty()
        )

        template.annotations.firstIsInstanceOrNull<SamWithReceiverAnnotations>()?.annotations?.let {
            annotationsForSamWithReceivers.put(it.map(::KotlinType))
        }

        @Suppress("DEPRECATION")
        fromLegacyTemplate(true)
        platform("JVM")
        hostConfiguration(hostConfiguration)
        displayName(template.simpleName!!)
        baseClass(KotlinType(template))
        compilerOptions.putIfAny(additionalCompilerArguments)
        // TODO: remove this exception when gradle switches to the new definitions and sets the property accordingly
        ide {
            acceptedLocations.put(scriptExpectedLocations)
        }
        asyncDependenciesResolver(dependencyResolver is AsyncDependenciesResolver || dependencyResolver is ApiChangeDependencyResolverWrapper)
        if (dependencyResolver != DependenciesResolver.NoDependencies) {
            refineConfiguration {
                onAnnotations(dependencyResolver.acceptedAnnotations.map(::KotlinType)) { context ->
                    refineWithResolver(dependencyResolver, context)
                }
            }
        }
        template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>()?.scriptFilePattern?.let {
            @Suppress("DEPRECATION_ERROR")
            fileNamePattern(it)
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

@Suppress("DEPRECATION")
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
    val environment = context.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]?.let {
        it[ScriptingHostConfiguration.getEnvironment]?.invoke()
    }.orEmpty()

    val (resolvedDeps, diagnostics) = runCatching {
        val result = dependencyResolver.resolve(ScriptContentsFromRefinementContext(context), environment)
        result.dependencies to result.reports.map { report ->
            ScriptDiagnostic(
                ScriptDiagnostic.unspecifiedError,
                report.message,
                mapLegacyDiagnosticSeverity(report.severity),
                context.script.locationId,
                mapLegacyScriptPosition(report.position)
            )
        }
    }.getOrElse {
        ScriptDependencies() to listOf(
            ScriptDiagnostic(
                code = ScriptDiagnostic.unspecifiedError,
                message = "Failed to resolve dependencies. resolver=$dependencyResolver of type=${dependencyResolver::class.simpleName}",
                sourcePath = context.script.locationId,
                exception = it
            )
        )
    }

    return if (resolvedDeps == null) ResultWithDiagnostics.Failure(diagnostics)
    else ScriptCompilationConfiguration(context.compilationConfiguration) {
        if (resolvedDeps.classpath.isNotEmpty()) {
            dependencies.append(JvmDependency(resolvedDeps.classpath))
        }
        defaultImports.append(resolvedDeps.imports)
        importScripts.append(resolvedDeps.scripts.map { FileScriptSource(it) })
        jvm {
            jdkHome.putIfNotNull(resolvedDeps.javaHome)
        }
        if (resolvedDeps.sources.isNotEmpty()) {
            ide {
                dependenciesSources.append(JvmDependency(resolvedDeps.sources))
            }
        }
    }.asSuccess(diagnostics)
}

fun resolverFromAnnotation(template: KClass<out Any>): DependenciesResolver {
    val defAnn = template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>() ?: return DependenciesResolver.NoDependencies

    return when (val resolver = instantiateResolver(defAnn.resolver)) {
        is AsyncDependenciesResolver -> AsyncDependencyResolverWrapper(resolver)
        is DependenciesResolver -> resolver
        else -> resolver?.let(::ApiChangeDependencyResolverWrapper)
    } ?: DependenciesResolver.NoDependencies
}

val DependenciesResolver.acceptedAnnotations: List<KClass<out Annotation>>
    get() {
        fun sameSignature(left: KFunction<*>, right: KFunction<*>): Boolean =
            left.name == right.name && left.parameters.size == right.parameters.size && left.parameters.zip(right.parameters).all {
                it.first.kind == KParameter.Kind.INSTANCE || it.first.name == it.second.name
            }

        val resolveFunctions = getResolveFunctions()

        return this.unwrap()::class.memberFunctions.filter { function -> resolveFunctions.any { sameSignature(function, it) } }
            .flatMap { it.annotations }.filterIsInstance<AcceptedAnnotations>().flatMap { it.supportedAnnotationClasses.toList() }
            .distinctBy { it.qualifiedName }
    }

internal interface DependencyResolverWrapper<T : ScriptDependenciesResolver> {
    val delegate: T
}

private fun ScriptDependenciesResolver.unwrap(): ScriptDependenciesResolver {
    return if (this is DependencyResolverWrapper<*>) delegate.unwrap() else this
}

// wraps AsyncDependenciesResolver to provide implementation for synchronous DependenciesResolver::resolve
private class AsyncDependencyResolverWrapper(
    override val delegate: AsyncDependenciesResolver,
) : AsyncDependenciesResolver, DependencyResolverWrapper<AsyncDependenciesResolver> {

    override fun resolve(
        scriptContents: ScriptContents, environment: Environment,
    ): DependenciesResolver.ResolveResult =
        @Suppress("DEPRECATION_ERROR") internalScriptingRunSuspend { delegate.resolveAsync(scriptContents, environment) }

    override suspend fun resolveAsync(
        scriptContents: ScriptContents, environment: Environment,
    ): DependenciesResolver.ResolveResult = delegate.resolveAsync(scriptContents, environment)
}
