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
import kotlin.script.experimental.api.ScriptingProperties

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScript(
    val name: String = "Kotlin script"
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScriptFileExtension(
    val extension: String
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Deprecated("Use KotlinScriptProperties", replaceWith = ReplaceWith("KotlinScriptProperties"))
annotation class KotlinScriptPropertiesFromList(
    val definitionProperties: KClass<out List<*>> // object or class filled in 0-ary constructor
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScriptProperties(
    val definitionProperties: KClass<out ScriptingProperties> // object or class filled in 0-ary constructor
)
