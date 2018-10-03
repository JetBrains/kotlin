/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.annotations

import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptCompilationConfiguration

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
 *        default - "Kotlin script"
 * @param fileExtension distinct filename extension for the script type being defined, stored in the configuration
 *        as {@link ScriptCompilationConfigurationKeys#fileExtension},
 *        default - "kts"
 * @param compilationConfiguration an object or a class with default constructor containing initial script compilation configuration
 *        default - {@link ScriptCompilationConfiguration#Default}
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
    val displayName: String = "Kotlin script",
    val fileExtension: String = "kts",
    val compilationConfiguration: KClass<out ScriptCompilationConfiguration> = ScriptCompilationConfiguration.Default::class
)

