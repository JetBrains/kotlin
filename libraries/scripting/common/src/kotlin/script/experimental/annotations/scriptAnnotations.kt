/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.annotations

import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptCompilationConfiguration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScript(
    val displayName: String = "Kotlin script",
    val fileExtension: String = "kts",
    val compilationConfiguration: KClass<out ScriptCompilationConfiguration> = ScriptCompilationConfiguration.Default::class
)

