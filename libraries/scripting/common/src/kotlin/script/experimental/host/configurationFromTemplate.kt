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

private const val SCRIPT_RUNTIME_TEMPLATES_PACKAGE = "kotlin.script.templates.standard"

@KotlinScript
private abstract class DummyScriptTemplate

/**
 * Creates the compilation configuration from annotated script base class
 * @param baseClassType the annotated script base class to construct the configuration from
 * @param hostConfiguration scripting host configuration properties
 * @param contextClass optional context class to extract classloading strategy from
 * @param body optional configuration function to add more properties to the compilation configuration
 */
fun createCompilationConfigurationFromTemplate(
    baseClassType: KotlinType,
    hostConfiguration: ScriptingHostConfiguration,
    contextClass: KClass<*> = ScriptCompilationConfiguration::class,
    body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ScriptCompilationConfiguration {

    val getScriptingClass = hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting 'getScriptingClass' parameter in the scripting host configuration")

    val baseClass: KClass<*> =
        try {
            getScriptingClass(baseClassType, contextClass, hostConfiguration)
        } catch (e: Throwable) {
            throw IllegalArgumentException("${ERROR_MSG_PREFIX}Unable to load base class $baseClassType", e)
        }
    val loadedBaseClassType = if (baseClass == baseClassType.fromClass) baseClassType else KotlinType(baseClass)

    val mainAnnotation = baseClass.findAnnotation<KotlinScript>()
        ?: run {
            // transitions to the new scripting API:
            // substituting annotations for standard templates from script-runtime
            when (baseClass.qualifiedName) {
                "$SCRIPT_RUNTIME_TEMPLATES_PACKAGE.SimpleScriptTemplate",
                "$SCRIPT_RUNTIME_TEMPLATES_PACKAGE.ScriptTemplateWithArgs",
                "$SCRIPT_RUNTIME_TEMPLATES_PACKAGE.ScriptTemplateWithBindings" -> DummyScriptTemplate::class
                else -> null
            }?.findAnnotation<KotlinScript>()
        }
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting KotlinScript annotation on the $baseClass")

    fun scriptConfigInstance(kclass: KClass<out ScriptCompilationConfiguration>): ScriptCompilationConfiguration = try {
        kclass.objectInstance ?: kclass.createInstance()
    } catch (e: Throwable) {
        throw IllegalArgumentException("$ILLEGAL_CONFIG_ANN_ARG: ${e.message}", e)
    }

    return ScriptCompilationConfiguration(scriptConfigInstance(mainAnnotation.compilationConfiguration)) {
        if (baseClass() == null) {
            baseClass(loadedBaseClassType)
        }
        if (fileExtension() == null) {
            fileExtension(mainAnnotation.fileExtension)
        }
        if (displayName() == null) {
            displayName(mainAnnotation.displayName)
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
