/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.definitions

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.annotations.KotlinScriptFileExtension
import kotlin.script.experimental.annotations.KotlinScriptProperties
import kotlin.script.experimental.annotations.KotlinScriptPropertiesFromList
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.TypedKey

private const val ERROR_MSG_PREFIX = "Unable to construct script definition: "

private const val ILLEGAL_CONFIG_ANN_ARG =
    "Illegal argument to KotlinScriptDefaultCompilationConfiguration annotation: expecting List-derived object or default-constructed class of configuration parameters"

fun createScriptDefinitionFromAnnotatedBaseClass(
    baseClassType: KotlinType,
    environment: ScriptingEnvironment,
    contextClass: KClass<*> = ScriptDefinition::class
): ScriptDefinition {

    val getScriptingClass = environment.getOrNull(ScriptingEnvironmentProperties.getScriptingClass)
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting 'getScriptingClass' parameter in the scripting environment")

    val baseClass: KClass<*> =
        try {
            getScriptingClass(baseClassType, contextClass, environment)
        } catch (e: Throwable) {
            throw IllegalArgumentException("${ERROR_MSG_PREFIX}Unable to load base class $baseClassType", e)
        }

    val mainAnnotation = baseClass.findAnnotation<KotlinScript>()
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting KotlinScript annotation on the $baseClass")

    val propertiesData = hashMapOf<TypedKey<*>, Any?>(ScriptDefinitionProperties.baseClass to baseClassType)

    propertiesData[ScriptDefinitionProperties.fileExtension] =
            baseClass.findAnnotation<KotlinScriptFileExtension>()?.let { it.extension }
            ?: mainAnnotation.extension
    propertiesData += ScriptDefinitionProperties.name to mainAnnotation.name
    baseClass.annotations.filterIsInstance(KotlinScriptPropertiesFromList::class.java).forEach { ann ->
        val params = try {
            ann.definitionProperties.objectInstance ?: ann.definitionProperties.createInstance()
        } catch (e: Throwable) {
            throw IllegalArgumentException(ILLEGAL_CONFIG_ANN_ARG, e)
        }
        params.forEach { param ->
            if (param !is Pair<*, *> || param.first !is TypedKey<*>)
                throw IllegalArgumentException("$ILLEGAL_CONFIG_ANN_ARG: invalid parameter $param")
            (param as Pair<TypedKey<*>, Any?>).let { (k, v) ->
                propertiesData[k] = v
            }
        }
    }

    fun scriptingPropsInstance(kclass: KClass<out ScriptingProperties>): ScriptingProperties = try {
        kclass.objectInstance ?: kclass.createInstance().also { it.setup() }
    } catch (e: Throwable) {
        throw IllegalArgumentException(ILLEGAL_CONFIG_ANN_ARG, e)
    }

    baseClass.annotations.filterIsInstance(KotlinScriptProperties::class.java).forEach { ann ->
        propertiesData.putAll(scriptingPropsInstance(ann.definitionProperties).data)
    }
    // TODO: chaining is lost here and above, fix it
    propertiesData.putAll(scriptingPropsInstance(mainAnnotation.properties).data)
    return ScriptingEnvironment.createOptimized(null, propertiesData)
}

