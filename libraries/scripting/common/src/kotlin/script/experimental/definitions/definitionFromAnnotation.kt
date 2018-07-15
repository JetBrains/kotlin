/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.definitions

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.script.experimental.annotations.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.basic.DummyEvaluator
import kotlin.script.experimental.util.TypedKey
import kotlin.script.experimental.util.chainPropertyBags

private const val ERROR_MSG_PREFIX = "Unable to construct script definition: "

private const val ILLEGAL_CONFIG_ANN_ARG =
    "Illegal argument to KotlinScriptDefaultCompilationConfiguration annotation: expecting List-derived object or default-constructed class of configuration parameters"

open class ScriptDefinitionFromAnnotatedBaseClass(
    protected val baseClassType: KotlinType,
    val environment: ScriptingEnvironment
) : ScriptDefinition {

    private val getScriptingClass = environment.getOrNull(ScriptingEnvironmentProperties.getScriptingClass)
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting 'getScriptingClass' parameter in the scripting environment")

    private val baseClass: KClass<*> =
        try {
            getScriptingClass(baseClassType, this::class, environment)
        } catch (e: Throwable) {
            throw IllegalArgumentException("${ERROR_MSG_PREFIX}Unable to load base class $baseClassType", e)
        }

    private val mainAnnotation = baseClass.findAnnotation<KotlinScript>()
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting KotlinScript annotation on the $baseClass")

    private val explicitDefinition: ScriptDefinition? =
        baseClass.findAnnotation<KotlinScriptDefinition>()?.definition.takeIf { it != this::class }?.let { it.instantiateScriptHandler() }

    override val properties = run {
        val baseProperties = chainPropertyBags(explicitDefinition?.properties, environment)
        val propertiesData = arrayListOf<Pair<TypedKey<*>, Any?>>(ScriptDefinitionProperties.baseClass to baseClassType)
        baseClass.findAnnotation<KotlinScriptFileExtension>()?.let {
            propertiesData += ScriptDefinitionProperties.fileExtension to it.extension
        }
        if (baseProperties.getOrNull(ScriptDefinitionProperties.name) == null) {
            propertiesData += ScriptDefinitionProperties.name to mainAnnotation.name
        }
        baseClass.annotations.filterIsInstance(KotlinScriptDefaultCompilationConfiguration::class.java).forEach { ann ->
            val params = try {
                ann.compilationConfiguration.objectInstance ?: ann.compilationConfiguration.createInstance()
            } catch (e: Throwable) {
                throw IllegalArgumentException(ILLEGAL_CONFIG_ANN_ARG, e)
            }
            params.forEach { param ->
                if (param !is Pair<*, *> || param.first !is TypedKey<*>)
                    throw IllegalArgumentException("$ILLEGAL_CONFIG_ANN_ARG: invalid parameter $param")
                propertiesData.add(param as Pair<TypedKey<*>, Any?>)
            }
        }
        ScriptingEnvironment(baseProperties, propertiesData)
    }

    override val evaluator =
        baseClass.findAnnotation<KotlinScriptEvaluator>()?.evaluator?.instantiateScriptHandler()
            ?: explicitDefinition?.evaluator
            ?: DummyEvaluator::class.instantiateScriptHandler()

    private fun <T : Any> KClass<T>.instantiateScriptHandler(): T {
        val klass: KClass<T> = try {
            getScriptingClass(KotlinType(this), this@ScriptDefinitionFromAnnotatedBaseClass::class, environment) as KClass<T>
        } catch (e: Throwable) {
            throw IllegalArgumentException("${ERROR_MSG_PREFIX}Unable to load handler $this: $e", e)
        }
        try {
            // TODO: fix call after deciding on constructor parameters
            return klass.objectInstance ?: klass.primaryConstructor!!.call(properties)
        } catch (e: Throwable) {
            throw IllegalArgumentException("${ERROR_MSG_PREFIX}Unable to instantiate handler $this: $e", e)
        }
    }
}

