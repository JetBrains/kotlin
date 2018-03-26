/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.definitions

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.annotations.KotlinScriptCompilationConfigurator
import kotlin.script.experimental.annotations.KotlinScriptEvaluator
import kotlin.script.experimental.annotations.KotlinScriptFileExtension
import kotlin.script.experimental.api.*
import kotlin.script.experimental.basic.DummyEvaluator
import kotlin.script.experimental.basic.PassThroughCompilationConfigurator

private const val ERROR_MSG_PREFIX = "Unable to construct script definition: "

open class ScriptDefinitionFromAnnotatedBaseClass(val environment: ScriptingEnvironment) : ScriptDefinition {

    private val baseClass: KClass<*> = environment.getOrNull(ScriptingEnvironmentParams.baseClass)
            ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting baseClass parameter in the scripting environment")

    private val mainAnnotation = baseClass.findAnnotation<KotlinScript>()
            ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting KotlinScript on the $baseClass")

    private val explicitDefinition: ScriptDefinition? =
        mainAnnotation.definition.takeIf { it != this::class }?.let { it.instantiateScriptHandler() }

    override val properties = (explicitDefinition?.properties ?: ScriptDefinitionPropertiesBag()).also { properties ->
        val toAdd = arrayListOf<Pair<TypedKey<*>, Any>>()
        baseClass.findAnnotation<KotlinScriptFileExtension>()?.let { toAdd += ScriptDefinitionProperties.fileExtension to it }
        if (properties.getOrNull(ScriptDefinitionProperties.name) == null) {
            toAdd += ScriptDefinitionProperties.name to baseClass.simpleName!!
        }
        properties.cloneWith(toAdd)
    }

    override val compilationConfigurator =
        baseClass.findAnnotation<KotlinScriptCompilationConfigurator>()?.compilationConfigurator?.instantiateScriptHandler()
                ?: explicitDefinition?.compilationConfigurator
                ?: PassThroughCompilationConfigurator::class.instantiateScriptHandler()

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

