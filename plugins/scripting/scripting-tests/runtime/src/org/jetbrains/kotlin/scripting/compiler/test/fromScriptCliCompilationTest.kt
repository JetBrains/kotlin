/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath

@KotlinScript(
    fileExtension = "req1.kts",
    compilationConfiguration = TestScriptWithRequireConfiguration::class
)
abstract class TestScriptWithRequire

object TestScriptWithRequireConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(Import::class, DependsOn::class)
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
        refineConfiguration {
            onAnnotations(Import::class, DependsOn::class) { context: ScriptConfigurationRefinementContext ->
                val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
                val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.map { it.annotation }
                val sources = annotations
                    ?.flatMap {
                        (it as? Import)?.sources?.map { sourceName ->
                            FileScriptSource(scriptBaseDir?.resolve(sourceName) ?: File(sourceName))
                        } ?: emptyList()
                    }
                val deps = annotations
                    ?.mapNotNull {
                        (it as? DependsOn)?.path?.let(::File)
                    }
                ScriptCompilationConfiguration(context.compilationConfiguration) {
                    if (sources?.isNotEmpty() == true) importScripts.append(sources)
                    if (deps != null) updateClasspath(deps)
                }.asSuccess()
            }
        }
    }
)

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Import(vararg val sources: String)
