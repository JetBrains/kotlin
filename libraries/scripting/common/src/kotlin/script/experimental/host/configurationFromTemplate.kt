/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.host

import kotlin.reflect.KClass
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.PropertiesCollection

private const val ERROR_MSG_PREFIX = "Unable to construct script definition: "

private const val ILLEGAL_CONFIG_ANN_ARG =
    "Illegal argument compilationConfiguration of the KotlinScript annotation: expecting an object or default-constructed class derived from ScriptCompilationConfiguration"

private const val SCRIPT_RUNTIME_TEMPLATES_PACKAGE = "kotlin.script.templates.standard"

@KotlinScript
private abstract class DummyScriptTemplate

/**
 * Creates compilation configuration from annotated script base class
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

    val templateClass: KClass<*> = baseClassType.getTemplateClass(hostConfiguration, contextClass)

    val mainAnnotation: KotlinScript = templateClass.kotlinScriptAnnotation

    return ScriptCompilationConfiguration(scriptConfigInstance(mainAnnotation.compilationConfiguration)) {
        hostConfiguration(hostConfiguration)
        propertiesFromTemplate(templateClass, baseClassType, mainAnnotation)
        body()
    }
}

/**
 * Creates evaluation configuration from annotated script base class
 * @param baseClassType the annotated script base class to construct the configuration from
 * @param hostConfiguration scripting host configuration properties
 * @param contextClass optional context class to extract classloading strategy from
 * @param body optional configuration function to add more properties to the evaluation configuration
 */
fun createEvaluationConfigurationFromTemplate(
    baseClassType: KotlinType,
    hostConfiguration: ScriptingHostConfiguration,
    contextClass: KClass<*> = ScriptEvaluationConfiguration::class,
    body: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
): ScriptEvaluationConfiguration {

    val templateClass: KClass<*> = baseClassType.getTemplateClass(hostConfiguration, contextClass)

    val mainAnnotation = templateClass.kotlinScriptAnnotation

    return ScriptEvaluationConfiguration(scriptConfigInstance(mainAnnotation.evaluationConfiguration)) {
        hostConfiguration(hostConfiguration)
        body()
    }
}

private fun ScriptCompilationConfiguration.Builder.propertiesFromTemplate(
    templateClass: KClass<*>, baseClassType: KotlinType, mainAnnotation: KotlinScript
) {
    if (baseClass() == null) {
        baseClass(if (templateClass == baseClassType.fromClass) baseClassType else KotlinType(templateClass))
    }
    if (fileExtension() == null) {
        fileExtension(mainAnnotation.fileExtension)
    }
    if (displayName() == null) {
        displayName(mainAnnotation.displayName)
    }
}

private val KClass<*>.kotlinScriptAnnotation: KotlinScript
    get() = findAnnotation()
        ?: when (this@kotlinScriptAnnotation.qualifiedName) {
            // transitions to the new scripting API: substituting annotations for standard templates from script-runtime
            "$SCRIPT_RUNTIME_TEMPLATES_PACKAGE.SimpleScriptTemplate",
            "$SCRIPT_RUNTIME_TEMPLATES_PACKAGE.ScriptTemplateWithArgs",
            "$SCRIPT_RUNTIME_TEMPLATES_PACKAGE.ScriptTemplateWithBindings" -> DummyScriptTemplate::class.findAnnotation<KotlinScript>()
            else -> null
        }
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting KotlinScript annotation on the ${this}")

private fun KotlinType.getTemplateClass(hostConfiguration: ScriptingHostConfiguration, contextClass: KClass<*>): KClass<*> {
    val getScriptingClass = hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting 'getScriptingClass' parameter in the scripting host configuration")

    return try {
        getScriptingClass(this, contextClass, hostConfiguration)
    } catch (e: Throwable) {
        throw IllegalArgumentException("${ERROR_MSG_PREFIX}Unable to load base class ${this}", e)
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

private fun <T : PropertiesCollection> scriptConfigInstance(kclass: KClass<out T>): T = try {
    kclass.objectInstance ?: kclass.createInstance()
} catch (e: Throwable) {
    throw IllegalArgumentException("$ILLEGAL_CONFIG_ANN_ARG: ${e.message}", e)
}

