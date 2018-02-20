/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.definitions

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptDefinition

open class ScriptDefinitionFromAnnotatedBaseClass(final override val baseClass: KClass<*>) : ScriptDefinition {
    private val annotation = baseClass.java.getAnnotation(KotlinScript::class.java)
            ?: throw IllegalArgumentException("Expecting KotlinScript on the $baseClass")

    override val selector = annotation.selector.instantiateScriptHandler()
    override val configurator = annotation.configurator.instantiateScriptHandler()
    override val runner = annotation.runner.instantiateScriptHandler()

    private fun <T : Any> KClass<T>.instantiateScriptHandler(): T {
        val fqn = this.qualifiedName!!
        val klass: KClass<T> = (baseClass.java.classLoader.loadClass(fqn) as Class<T>).kotlin
        // TODO: fix call after deciding on constructor parameters
        return klass.objectInstance ?: klass.primaryConstructor!!.call(baseClass)
    }
}

