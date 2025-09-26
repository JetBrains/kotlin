/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.scripting.resolve.ApiChangeDependencyResolverWrapper
import org.jetbrains.kotlin.scripting.resolve.AsyncDependencyResolverWrapper
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.location.ScriptExpectedLocations
import kotlin.script.templates.AcceptedAnnotations
import kotlin.script.templates.DEFAULT_SCRIPT_FILE_PATTERN
import kotlin.script.templates.ScriptTemplateDefinition

@Deprecated("Use 'ScriptDefinition' instead", level = DeprecationLevel.WARNING)
internal class LegacyKotlinScriptDefinitionFromAnnotatedTemplate(
    val template: KClass<out Any>,
    val environment: Map<String, Any?>? = null,
) : UserDataHolderBase() {

    val scriptFilePattern by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val pattern =
            takeUnlessError { template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>()?.scriptFilePattern }
                ?: DEFAULT_SCRIPT_FILE_PATTERN
        Regex(pattern)
    }

    val name = template.simpleName!!

    val annotationsForSamWithReceivers: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        takeUnlessError { template.annotations.firstIsInstanceOrNull<kotlin.script.extensions.SamWithReceiverAnnotations>()?.annotations?.toList() }
            ?: emptyList()
    }

    fun isScript(fileName: String): Boolean = scriptFilePattern.matches(fileName)

    val dependencyResolver: DependenciesResolver by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolverFromAnnotation(template) ?: DependenciesResolver.NoDependencies
    }

    private fun resolverFromAnnotation(template: KClass<out Any>): DependenciesResolver? {
        val defAnn = takeUnlessError {
            template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>()
        } ?: return null

        return when (val resolver = instantiateResolver(defAnn.resolver)) {
            is AsyncDependenciesResolver -> AsyncDependencyResolverWrapper(resolver)
            is DependenciesResolver -> resolver
            else -> resolver?.let(::ApiChangeDependencyResolverWrapper)
        }
    }

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

    val acceptedAnnotations: List<KClass<out Annotation>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        fun sameSignature(left: KFunction<*>, right: KFunction<*>): Boolean =
            left.name == right.name &&
                    left.parameters.size == right.parameters.size &&
                    left.parameters.zip(right.parameters).all {
                        it.first.kind == KParameter.Kind.INSTANCE ||
                                it.first.name == it.second.name
                    }

        val resolveFunctions = getResolveFunctions()

        dependencyResolver.unwrap()::class.memberFunctions
            .asSequence()
            .filter { function -> resolveFunctions.any { sameSignature(function, it) } }
            .flatMap { it.annotations }
            .filterIsInstance<AcceptedAnnotations>()
            .flatMap { it.supportedAnnotationClasses.toList() }
            .distinctBy { it.qualifiedName }
            .toList()
    }

    private fun getResolveFunctions(): List<KFunction<*>> {
        // DependenciesResolver::resolve, ScriptDependenciesResolver::resolve, AsyncDependenciesResolver::resolveAsync
        return AsyncDependenciesResolver::class.memberFunctions.filter { it.name == "resolve" || it.name == "resolveAsync" }.also {
            assert(it.size == 3) {
                AsyncDependenciesResolver::class.memberFunctions
                    .joinToString(prefix = "${AsyncDependenciesResolver::class.qualifiedName} api changed, fix this code") { it.name }
            }
        }
    }

    @Deprecated("temporary workaround for missing functionality, will be replaced by the new API soon")
    val additionalCompilerArguments: Iterable<String>? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        takeUnlessError {
            template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateAdditionalCompilerArguments>()?.let {
                val res = it.provider.primaryConstructor?.call(it.arguments.asIterable())
                res
            }
        }?.getAdditionalCompilerArguments(environment)
    }

    @Suppress("DEPRECATION")
    val scriptExpectedLocations: List<kotlin.script.experimental.location.ScriptExpectedLocation> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        takeUnlessError {
            template.annotations.firstIsInstanceOrNull<ScriptExpectedLocations>()
        }?.value?.toList() ?: listOf(
            kotlin.script.experimental.location.ScriptExpectedLocation.SourcesOnly,
            kotlin.script.experimental.location.ScriptExpectedLocation.TestsOnly
        )
    }

    override fun toString(): String = "KotlinScriptDefinitionFromAnnotatedTemplate - ${template.simpleName}"

    private inline fun <T> takeUnlessError(reportError: Boolean = true, body: () -> T?): T? =
        try {
            body()
        } catch (ex: Throwable) {
            if (reportError) {
                log.error("Invalid script template: " + template.qualifiedName, ex)
            } else {
                log.warn("Invalid script template: " + template.qualifiedName, ex)
            }
            null
        }

    companion object {
        internal val log = Logger.getInstance(LegacyKotlinScriptDefinitionFromAnnotatedTemplate::class.java)
    }
}

interface DependencyResolverWrapper<T : ScriptDependenciesResolver> {
    val delegate: T
}

fun ScriptDependenciesResolver.unwrap(): ScriptDependenciesResolver {
    return if (this is DependencyResolverWrapper<*>) delegate.unwrap() else this
}
