/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.resolve

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.experimental.location.ScriptExpectedLocations
import kotlin.script.templates.AcceptedAnnotations
import kotlin.script.templates.DEFAULT_SCRIPT_FILE_PATTERN
import kotlin.script.templates.ScriptTemplateDefinition

open class KotlinScriptDefinitionFromAnnotatedTemplate(
        template: KClass<out Any>,
        val environment: Map<String, Any?>? = null,
        val templateClasspath: List<File> = emptyList()
) : KotlinScriptDefinition(template) {
    val scriptFilePattern by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val pattern =
            takeUnlessError {
                val ann = template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateDefinition>()
                ann?.scriptFilePattern
            }
                    ?: takeUnlessError { template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>()?.scriptFilePattern }
                    ?: DEFAULT_SCRIPT_FILE_PATTERN
        Regex(pattern)
    }

    override val dependencyResolver: DependenciesResolver by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolverFromAnnotation(template) ?:
        DependenciesResolver.NoDependencies
    }

    private fun resolverFromAnnotation(template: KClass<out Any>): DependenciesResolver? {
        val defAnn = takeUnlessError {
            template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateDefinition>()
        } ?: return null

        val resolver = instantiateResolver(defAnn.resolver)
        return when (resolver) {
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
        }
        catch (ex: ClassCastException) {
            log.warn("[kts] Script def error ${ex.message}")
            return null
        }
    }

    private val samWithReceiverAnnotations: List<String>? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        takeUnlessError { template.annotations.firstIsInstanceOrNull<kotlin.script.extensions.SamWithReceiverAnnotations>()?.annotations?.toList() }
    }

    override val acceptedAnnotations: List<KClass<out Annotation>> by lazy(LazyThreadSafetyMode.PUBLICATION) {

        fun sameSignature(left: KFunction<*>, right: KFunction<*>): Boolean =
                left.name == right.name &&
                left.parameters.size == right.parameters.size &&
                left.parameters.zip(right.parameters).all {
                    it.first.kind == KParameter.Kind.INSTANCE ||
                    it.first.name == it.second.name
                }

        val resolveFunctions = getResolveFunctions()

        dependencyResolver.unwrap()::class.memberFunctions
                .filter { function -> resolveFunctions.any { sameSignature(function, it) } }
                .flatMap { it.annotations }
                .filterIsInstance<AcceptedAnnotations>()
                .flatMap { it.supportedAnnotationClasses.toList() }
                .distinctBy { it.qualifiedName }
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

    override val scriptExpectedLocations: List<ScriptExpectedLocation> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        takeUnlessError {
            template.annotations.firstIsInstanceOrNull<ScriptExpectedLocations>()
        }?.value?.toList() ?: super.scriptExpectedLocations
    }

    override val name = template.simpleName!!

    override fun isScript(fileName: String): Boolean =
        scriptFilePattern.matches(fileName)

    // TODO: implement other strategy - e.g. try to extract something from match with ScriptFilePattern
    override fun getScriptName(script: KtScript): Name = NameUtils.getScriptNameForFile(script.containingKtFile.name)

    override fun toString(): String = "KotlinScriptDefinitionFromAnnotatedTemplate - ${template.simpleName}"

    override val annotationsForSamWithReceivers: List<String>
        get() = samWithReceiverAnnotations ?: super.annotationsForSamWithReceivers

    @Deprecated("temporary workaround for missing functionality, will be replaced by the new API soon")
    override val additionalCompilerArguments: Iterable<String>? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        takeUnlessError {
            template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateAdditionalCompilerArguments>()?.let {
                val res = it.provider.primaryConstructor?.call(it.arguments.asIterable())
                res
            }
        }?.getAdditionalCompilerArguments(environment)
    }

    private inline fun<T> takeUnlessError(reportError: Boolean = true, body: () -> T?): T? =
            try {
                body()
            }
            catch (ex: Throwable) {
                if (reportError) {
                    log.error("Invalid script template: " + template.qualifiedName, ex)
                }
                else {
                    log.warn("Invalid script template: " + template.qualifiedName, ex)
                }
                null
            }

    companion object {
        internal val log = Logger.getInstance(KotlinScriptDefinitionFromAnnotatedTemplate::class.java)
    }
}

interface DependencyResolverWrapper<T : ScriptDependenciesResolver> {
    val delegate: T
}

fun ScriptDependenciesResolver.unwrap(): ScriptDependenciesResolver {
    return if (this is DependencyResolverWrapper<*>) delegate.unwrap() else this
}
