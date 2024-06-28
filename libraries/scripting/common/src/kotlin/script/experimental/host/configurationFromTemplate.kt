/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.host

import kotlin.reflect.KClass
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.PropertiesCollection

/**
 * Script definition combines configuration data for script compilation and evaluation
 */
data class ScriptDefinition(
    val compilationConfiguration: ScriptCompilationConfiguration,
    val evaluationConfiguration: ScriptEvaluationConfiguration
)

/**
 * Creates script compilation and evaluation configuration from annotated script base class
 * @param baseClassType the annotated script base class to construct the configuration from
 * @param baseHostConfiguration base scripting host configuration properties
 * @param contextClass optional context class to extract classloading strategy from
 * @param compilation optional configuration function to add more properties to the compilation configuration
 * @param evaluation optional configuration function to add more properties to the evaluation configuration
 */
fun createScriptDefinitionFromTemplate(
    baseClassType: KotlinType,
    baseHostConfiguration: ScriptingHostConfiguration,
    contextClass: KClass<*> = ScriptDefinition::class,
    compilation: ScriptCompilationConfiguration.Builder.() -> Unit = {},
    evaluation: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
): ScriptDefinition {
    val templateClass: KClass<*> = baseClassType.getTemplateClass(baseHostConfiguration, contextClass)
    val mainAnnotation = templateClass.kotlinScriptAnnotation

    val hostConfiguration = constructHostConfiguration(mainAnnotation.hostConfiguration, baseHostConfiguration) {}

    val compilationConfiguration =
        constructCompilationConfiguration(mainAnnotation, hostConfiguration, templateClass, baseClassType, compilation)
    val evaluationConfiguration = constructEvaluationConfiguration(mainAnnotation, hostConfiguration, evaluation)

    return ScriptDefinition(compilationConfiguration, evaluationConfiguration)
}

/**
 * Creates compilation configuration from annotated script base class
 * NOTE: it is preferable to use createScriptDefinitionFromTemplate for creating all configurations at once
 * @param baseClassType the annotated script base class to construct the configuration from
 * @param baseHostConfiguration scripting host configuration properties
 * @param contextClass optional context class to extract classloading strategy from
 * @param body optional configuration function to add more properties to the compilation configuration
 */
fun createCompilationConfigurationFromTemplate(
    baseClassType: KotlinType,
    baseHostConfiguration: ScriptingHostConfiguration,
    contextClass: KClass<*> = ScriptCompilationConfiguration::class,
    body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ScriptCompilationConfiguration {
    val templateClass: KClass<*> = baseClassType.getTemplateClass(baseHostConfiguration, contextClass)
    val mainAnnotation = templateClass.kotlinScriptAnnotation

    val hostConfiguration = constructHostConfiguration(mainAnnotation.hostConfiguration, baseHostConfiguration) {}

    return constructCompilationConfiguration(mainAnnotation, hostConfiguration, templateClass, baseClassType, body)
}

/**
 * Creates evaluation configuration from annotated script base class
 * NOTE: it is preferable to use createScriptDefinitionFromTemplate for creating all configurations at once
 * @param baseClassType the annotated script base class to construct the configuration from
 * @param baseHostConfiguration scripting host configuration properties
 * @param contextClass optional context class to extract classloading strategy from
 * @param body optional configuration function to add more properties to the evaluation configuration
 */
fun createEvaluationConfigurationFromTemplate(
    baseClassType: KotlinType,
    baseHostConfiguration: ScriptingHostConfiguration,
    contextClass: KClass<*> = ScriptEvaluationConfiguration::class,
    body: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
): ScriptEvaluationConfiguration {
    val templateClass: KClass<*> = baseClassType.getTemplateClass(baseHostConfiguration, contextClass)
    val mainAnnotation = templateClass.kotlinScriptAnnotation

    val hostConfiguration = constructHostConfiguration(mainAnnotation.hostConfiguration, baseHostConfiguration) {}

    return constructEvaluationConfiguration(mainAnnotation, hostConfiguration, body)
}

private const val ERROR_MSG_PREFIX = "Unable to construct script definition: "

private const val SCRIPT_RUNTIME_TEMPLATES_PACKAGE = "kotlin.script.templates.standard"

@KotlinScript
private abstract class DummyScriptTemplate

private fun constructCompilationConfiguration(
    mainAnnotation: KotlinScript,
    hostConfiguration: ScriptingHostConfiguration,
    templateClass: KClass<*>,
    baseClassType: KotlinType,
    body: ScriptCompilationConfiguration.Builder.() -> Unit
): ScriptCompilationConfiguration {
    val compilationConfigurationInstance = scriptConfigInstance(mainAnnotation.compilationConfiguration)
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Illegal argument compilationConfiguration of the KotlinScript annotation: expecting an object or default-constructable class derived from ScriptCompilationConfiguration")

    return ScriptCompilationConfiguration(compilationConfigurationInstance) {
        // TODO: consider deprecating host configuration updating here, it is better to do it via dedicated annotation parameter
        this.hostConfiguration.update { it.withDefaultsFrom(hostConfiguration) }
        propertiesFromTemplate(templateClass, baseClassType, mainAnnotation)
        body()
    }
}

private fun constructEvaluationConfiguration(
    mainAnnotation: KotlinScript,
    hostConfiguration: ScriptingHostConfiguration,
    body: ScriptEvaluationConfiguration.Builder.() -> Unit
): ScriptEvaluationConfiguration {
    val evaluationConfigurationInstance = scriptConfigInstance(mainAnnotation.evaluationConfiguration)
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Illegal argument evaluationConfiguration of the KotlinScript annotation: expecting an object or default-constructable class derived from ScriptEvaluationConfiguration")

    return ScriptEvaluationConfiguration(evaluationConfigurationInstance) {
        // TODO: consider deprecating host configuration updating here, it is better to do it via dedicated annotation parameter
        this.hostConfiguration.update { it.withDefaultsFrom(hostConfiguration) }
        body()
    }
}

private fun constructHostConfiguration(
    hostConfigurationKClass: KClass<out ScriptingHostConfiguration>,
    baseHostConfiguration: ScriptingHostConfiguration,
    body: ScriptingHostConfiguration.Builder.() -> Unit
): ScriptingHostConfiguration {
    if (hostConfigurationKClass == ScriptingHostConfiguration::class)
        return ScriptingHostConfiguration(body).withDefaultsFrom(baseHostConfiguration)

    val singleArgConstructor = hostConfigurationKClass.java.constructors.singleOrNull {
        it.parameters.isNotEmpty() && it.parameters.first().type.isAssignableFrom(ScriptingHostConfiguration::class.java)
    }

    val hostConfigurationInstance =
        if (singleArgConstructor != null) singleArgConstructor.newInstance(baseHostConfiguration) as ScriptingHostConfiguration
        else scriptConfigInstance(hostConfigurationKClass)
            ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Illegal argument hostConfiguration of the KotlinScript annotation: expecting an object or a class derived from ScriptingHostConfiguration constructable without arguments or from a base configuration")

    return hostConfigurationInstance.with {
        body()
    }.withDefaultsFrom(baseHostConfiguration)
}

private fun ScriptCompilationConfiguration.Builder.propertiesFromTemplate(
    templateClass: KClass<*>, baseClassType: KotlinType, mainAnnotation: KotlinScript
) {
    baseClass.replaceOnlyDefault(if (templateClass == baseClassType.fromClass) baseClassType else KotlinType(templateClass))
    fileExtension.replaceOnlyDefault(mainAnnotation.fileExtension)
    // TODO: remove this exception when gradle switches to the new definitions and sets the property accordingly
    // possible gradle script extensions - see PrecompiledScriptTemplates.kt in the gradle repository
    if (get(fileExtension) in arrayOf("gradle.kts", "init.gradle.kts", "settings.gradle.kts")) {
        isStandalone(false)
    }
    @Suppress("DEPRECATION_ERROR")
    fileNamePattern.replaceOnlyDefault(mainAnnotation.filePathPattern)
    displayName.replaceOnlyDefault(mainAnnotation.displayName)
}

private val KClass<*>.kotlinScriptAnnotation: KotlinScript
    get() = findAnnotation()
        ?: when (this@kotlinScriptAnnotation.qualifiedName) {
            // Any is the default template, so use a default annotation
            Any::class.qualifiedName,
                // transitions to the new scripting API: substituting annotations for standard templates from script-runtime
            "$SCRIPT_RUNTIME_TEMPLATES_PACKAGE.SimpleScriptTemplate",
            "$SCRIPT_RUNTIME_TEMPLATES_PACKAGE.ScriptTemplateWithArgs",
            "$SCRIPT_RUNTIME_TEMPLATES_PACKAGE.ScriptTemplateWithBindings",
            -> DummyScriptTemplate::class.findAnnotation()
            else -> null
        }
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting KotlinScript annotation on the $this")

private fun KotlinType.getTemplateClass(hostConfiguration: ScriptingHostConfiguration, contextClass: KClass<*>): KClass<*> {
    val getScriptingClass = hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
        ?: throw IllegalArgumentException("${ERROR_MSG_PREFIX}Expecting 'getScriptingClass' parameter in the scripting host configuration")

    return try {
        getScriptingClass(this, contextClass, hostConfiguration)
    } catch (e: Throwable) {
        throw IllegalArgumentException("${ERROR_MSG_PREFIX}Unable to load base class $this", e)
    }
}

private inline fun <reified T : Annotation> KClass<*>.findAnnotation(): T? =
    @Suppress("UNCHECKED_CAST")
    this.java.annotations.firstOrNull { it is T } as T?

private inline fun <reified T : PropertiesCollection> scriptConfigInstance(kclass: KClass<out T>): T? =
    kclass.objectInstance ?: run {
        val noArgsConstructor = kclass.java.constructors.singleOrNull { it.parameters.isEmpty() }
        noArgsConstructor?.let { it.newInstance() as T }
    }

