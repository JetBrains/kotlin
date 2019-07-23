/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.annotations

import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration

/**
 * The annotation for declaring a script definition (template)
 * A class marked with this annotation is considered a script definition and could be used in script definitions discovery or
 * referenced explicitly by FQName for compiler or IDE. The compilation configuration is constructed from the annotated class
 * (it is used as a compiled script super class) and annotation parameters.
 *
 * It is important to specify [fileExtension] property via the parameter for
 * optimal discovery and definitions loading performance.
 *
 * @param displayName script definition display name, stored as {@link ScriptCompilationConfigurationKeys#displayName},
 *        default - empty - use annotated class name
 * @param fileExtension distinct filename extension for the script type being defined, stored in the configuration
 *        as {@link ScriptCompilationConfigurationKeys#fileExtension},
 *        default - "kts"
 * @param filePathPattern additional (to the filename extension) RegEx pattern with that the script file path is checked
 *        as {@link ScriptCompilationConfigurationKeys#filePathPattern},
 *        default - empty - pattern is not used
 * @param compilationConfiguration an object or a class with default constructor containing initial script compilation configuration
 *        default - {@link ScriptCompilationConfiguration#Default}
 * @param evaluationConfiguration an object or a class with default constructor containing initial script evaluation configuration
 *        default - {@link ScriptEvaluationConfiguration#Default}
 *
 * Simple usage example:
 * <pre>
 * {@code
 *   object MyScriptConfiguration : ScriptCompilationConfiguration({
 *     defaultImports("java.io.File")
 *   })
 *
 *   @KotlinScript(fileExtension("myscript.kts"))
 *   abstract class MyScriptBaseClass
 * }
 * </pre>
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScript(
    val displayName: String = "",
    val fileExtension: String = "kts",
    val filePathPattern: String = "",
    val compilationConfiguration: KClass<out ScriptCompilationConfiguration> = ScriptCompilationConfiguration.Default::class,
    val evaluationConfiguration: KClass<out ScriptEvaluationConfiguration> = ScriptEvaluationConfiguration.Default::class
)

