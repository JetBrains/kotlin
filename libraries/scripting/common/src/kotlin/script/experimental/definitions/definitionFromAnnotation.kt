/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.definitions

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.script.experimental.annotations.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.basic.AnnotationsBasedCompilationConfigurator
import kotlin.script.experimental.basic.DummyEvaluator
import kotlin.script.experimental.util.TypedKey

private const val ERROR_MSG_PREFIX = "Unable to construct script definition: "

open class ScriptDefinitionFromAnnotatedBaseClass(val environment: ScriptingEnvironment) : ScriptDefinition {

    private val baseClass: KClass<*> = environment.getOrNull(ScriptingEnvironmentProperties.baseClass)
            ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting baseClass parameter in the scripting environment")

    private val mainAnnotation = baseClass.findAnnotation<KotlinScript>()
            ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting KotlinScript annotation on the $baseClass")

    private val explicitDefinition: ScriptDefinition? =
        baseClass.findAnnotation<KotlinScriptDefinition>()?.definition.takeIf { it != this::class }?.let { it.instantiateScriptHandler() }

    override val properties = (explicitDefinition?.properties ?: ScriptingEnvironment()).also { properties ->
        val toAdd = arrayListOf<Pair<TypedKey<*>, Any>>()
        baseClass.findAnnotation<KotlinScriptFileExtension>()?.let { toAdd += ScriptDefinitionProperties.fileExtension to it }
        if (properties.getOrNull(ScriptDefinitionProperties.name) == null) {
            toAdd += ScriptDefinitionProperties.name to mainAnnotation.name
        }
        ScriptingEnvironment(properties, toAdd)
    }

    override val compilationConfigurator =
        baseClass.findAnnotation<KotlinScriptCompilationConfigurator>()?.compilationConfigurator?.instantiateScriptHandler()
                ?: explicitDefinition?.compilationConfigurator
                ?: AnnotationsBasedCompilationConfigurator::class.instantiateScriptHandler()

    override val evaluator =
        baseClass.findAnnotation<KotlinScriptEvaluator>()?.evaluator?.instantiateScriptHandler()
                ?: explicitDefinition?.evaluator
                ?: DummyEvaluator::class.instantiateScriptHandler()

    private fun <T : Any> KClass<T>.instantiateScriptHandler(): T {
        val fqn = this.qualifiedName!!
        val klass: KClass<T> = (baseClass.java.classLoader.loadClass(fqn) as Class<T>).kotlin
        // TODO: fix call after deciding on constructor parameters
        return klass.objectInstance ?: klass.primaryConstructor!!.call(environment)
    }
}

