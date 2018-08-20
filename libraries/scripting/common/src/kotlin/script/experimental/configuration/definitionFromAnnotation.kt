/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.configuration

import kotlin.reflect.KClass
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.annotations.KotlinScriptFileExtension
import kotlin.script.experimental.annotations.KotlinScriptProperties
import kotlin.script.experimental.api.*

private const val ERROR_MSG_PREFIX = "Unable to construct script definition: "

private const val ILLEGAL_CONFIG_ANN_ARG =
    "Illegal argument compilationConfiguration of the KotlinScript annotation: expecting an object or default-constructed class derived from ScriptCompilationConfiguration"

fun createScriptCompilationConfigurationFromAnnotatedBaseClass(
    baseClassType: KotlinType,
    environment: ScriptingEnvironment,
    contextClass: KClass<*> = ScriptCompilationConfiguration::class,
    body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ScriptCompilationConfiguration {

    val getScriptingClass = environment[ScriptingEnvironment.getScriptingClass]
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting 'getScriptingClass' parameter in the scripting environment")

    val baseClass: KClass<*> =
        try {
            getScriptingClass(baseClassType, contextClass, environment)
        } catch (e: Throwable) {
            throw IllegalArgumentException("${ERROR_MSG_PREFIX}Unable to load base class $baseClassType", e)
        }
    val loadedBaseClassType = if (baseClass == baseClassType.fromClass) baseClassType else KotlinType(baseClass)

    val mainAnnotation = baseClass.findAnnotation<KotlinScript>()
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting KotlinScript annotation on the $baseClass")

    fun scriptingPropsInstance(kclass: KClass<out ScriptCompilationConfiguration>): ScriptCompilationConfiguration = try {
        kclass.objectInstance ?: kclass.createInstance()
    } catch (e: Throwable) {
        throw IllegalArgumentException(ILLEGAL_CONFIG_ANN_ARG, e)
    }

    return ScriptCompilationConfiguration {
        baseClass(loadedBaseClassType)
        fileExtension(baseClass.findAnnotation<KotlinScriptFileExtension>()?.extension ?: mainAnnotation.extension)
        displayName(mainAnnotation.name)

        include(scriptingPropsInstance(mainAnnotation.compilationConfiguration))

        baseClass.java.annotations.filterIsInstance(KotlinScriptProperties::class.java).forEach { ann ->
            include(scriptingPropsInstance(ann.compilationConfiguration))
        }
        body()
    }
}

private inline fun <reified T : Annotation> KClass<*>.findAnnotation(): T? =
    @Suppress("UNCHECKED_CAST")
    this.java.annotations.firstOrNull { it is T } as T?

private fun <T : Any> KClass<T>.createInstance(): T {
    // TODO: throw a meaningful exception
    val noArgsConstructor = java.constructors.singleOrNull { it.parameters.isEmpty() }
        ?: throw IllegalArgumentException("Class should have a single no-arg constructor: $this")

    @Suppress("UNCHECKED_CAST")
    return noArgsConstructor.newInstance() as T
}
