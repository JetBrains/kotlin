/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.definition

import kotlin.script.experimental.annotations.KotlinScript
import org.jetbrains.kotlin.scripting.definitions.getEnvironment
import org.jetbrains.kotlin.scripting.resolve.InvalidScriptResolverAnnotation
import org.jetbrains.kotlin.scripting.test.definition.gradleLike.CompiledKotlinBuildScript
import org.jetbrains.kotlin.scripting.test.definition.gradleLike.Project
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@Suppress("unused", "UNUSED_PARAMETER")
@KotlinScript(fileExtension = "test.kts", compilationConfiguration = ConfigurableTestScriptConfiguration::class)
abstract class ConfigurableTestScript(vararg val args: String)

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class AddTestImport(vararg val imports: String)

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class TestRefinementMarker(val name: String, val count: Int = 1)

class ConfigurableTestScriptConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(AddTestImport::class, TestRefinementMarker::class)
        refineConfiguration {
            onAnnotations(AddTestImport::class, TestRefinementMarker::class) { ctx ->
                val collected = ctx.collectedData?.get(ScriptCollectedData.collectedAnnotations).orEmpty()
                val invalid = collected.map { it.annotation }.filterIsInstance<InvalidScriptResolverAnnotation>()
                if (invalid.isNotEmpty()) {
                    makeFailureResult(
                        invalid.map { "Unable to construct the annotation ${it.name}: ${it.error?.message ?: it.annParams}".asErrorDiagnostics() }
                    )
                } else {
                    ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                        collected.forEach {
                            when (val annotation = it.annotation) {
                                is AddTestImport -> defaultImports.append(annotation.imports.toList())
                                is TestRefinementMarker ->
                                    defaultImports.append("test.marker.${annotation.name}.${annotation.count}")
                            }
                        }
                    }.asSuccess()
                }
            }
            beforeCompiling { ctx ->
                val hostConfiguration = ctx.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]!!
                val env = hostConfiguration[ScriptingHostConfiguration.getEnvironment]?.invoke()
                if (env == null) makeFailureResult("Unable to retrieve environment for the custom test script")
                else
                    ScriptCompilationConfiguration(ctx.compilationConfiguration) {
                        env["gradleLikeScript"]?.let {
                            @Suppress("UNCHECKED_CAST")
                            defaultImports("org.jetbrains.kotlin.scripting.test.definition.gradleLike.*")
                            jvm {
                                dependenciesFromCurrentContext(wholeClasspath = true)
                            }
                            baseClass(CompiledKotlinBuildScript::class)
                            implicitReceivers(Project::class)
                        }
                        env["defaultImports"]?.let {
                            @Suppress("UNCHECKED_CAST")
                            defaultImports.append(it as List<String>)
                        }
                        env["providedProperties"]?.let {
                            @Suppress("UNCHECKED_CAST")
                            (it as List<String>).forEach {
                                it.split(Regex(" *: *")).let {
                                    providedProperties.append(listOf(it.first() to KotlinType(it.last())))
                                }
                            }
                        }
                    }.asSuccess()
            }
        }
    }
)
