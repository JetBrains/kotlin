/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.annotations

import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptCompilationConfigurator
import kotlin.script.experimental.api.ScriptDefinition
import kotlin.script.experimental.api.ScriptEvaluator
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScript(
    val definition: KClass<out ScriptDefinition> = ScriptDefinitionFromAnnotatedBaseClass::class
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScriptFileExtension(
    val extension: String
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScriptCompilationConfigurator(
    val compilationConfigurator: KClass<out ScriptCompilationConfigurator>
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScriptEvaluator(
    val evaluator: KClass<out ScriptEvaluator<*>>
)

