/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.host

import kotlin.reflect.KClass
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*

private const val ERROR_MSG_PREFIX = "Unable to construct script definition: "

private const val ILLEGAL_CONFIG_ANN_ARG =
    "Illegal argument compilationConfiguration of the KotlinScript annotation: expecting an object or default-constructed class derived from ScriptCompilationConfiguration"

fun createScriptCompilationConfigurationFromAnnotatedBaseClass(
    baseClassType: KotlinType,
    hostConfiguration: ScriptingHostConfiguration,
    contextClass: KClass<*> = ScriptCompilationConfiguration::class,
    body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ScriptCompilationConfiguration {

    val getScriptingClass = hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting 'getScriptingClass' parameter in the scripting environment")

    val baseClass: KClass<*> =
        try {
            getScriptingClass(baseClassType, contextClass, hostConfiguration)
        } catch (e: Throwable) {
            throw IllegalArgumentException("${ERROR_MSG_PREFIX}Unable to load base class $baseClassType", e)
        }
    val loadedBaseClassType = if (baseClass == baseClassType.fromClass) baseClassType else KotlinType(baseClass)

    val mainAnnotation = baseClass.findAnnotation<KotlinScript>()
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting KotlinScript annotation on the $baseClass")

    fun scriptConfigInstance(kclass: KClass<out ScriptCompilationConfiguration>): ScriptCompilationConfiguration = try {
        kclass.objectInstance ?: kclass.createInstance()
    } catch (e: Throwable) {
        throw IllegalArgumentException(ILLEGAL_CONFIG_ANN_ARG, e)
    }

    return ScriptCompilationConfiguration(scriptConfigInstance(mainAnnotation.compilationConfiguration)) {
        if (baseClass() == null) {
            baseClass(loadedBaseClassType)
        }
        if (fileExtension() == null) {
            fileExtension(mainAnnotation.extension)
        }
        if (displayName() == null) {
            displayName(mainAnnotation.name)
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
