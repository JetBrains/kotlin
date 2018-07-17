/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.script.experimental.util.ChainedPropertyBag
import kotlin.script.experimental.util.typedKey

typealias ScriptDefinition = ChainedPropertyBag

object ScriptDefinitionProperties : PropertiesGroup {

    val name by typedKey<String>() // Name of the script type, by default "Kotlin script"

    val fileExtension by typedKey<String>() // default: "kts"

    val baseClass by typedKey<KotlinType>() // script base class

    val scriptBodyTarget by typedKey<ScriptBodyTarget>()

    val scriptImplicitReceivers by typedKey<List<KotlinType>>() // in the order from outer to inner scope

    val contextVariables by typedKey<Map<String, KotlinType>>() // external variables

    val defaultImports by typedKey<List<String>>()

    val restrictions by typedKey<List<ResolvingRestrictionRule>>()

    val importedScripts by typedKey<List<ScriptSource>>()

    val dependencies by typedKey<List<ScriptDependency>>()

    val generatedClassAnnotations by typedKey<List<Annotation>>()

    val generatedMethodAnnotations by typedKey<List<Annotation>>()

    val compilerOptions by typedKey<List<String>>() // Q: CommonCompilerOptions instead?

    val refineConfigurationHandler by typedKey<RefineScriptCompilationConfigurationHandler>() // dynamic configurator

    val refineConfigurationBeforeParsing by typedKey<Boolean>() // default: false

    val refineConfigurationOnAnnotations by typedKey<List<KotlinType>>()

    val refineConfigurationOnSections by typedKey<List<String>>()

    // DSL:

    val refineConfiguration by propertiesBuilder<RefineConfigurationBuilder>()
}

// DSL --------------------

val ScriptingProperties.scriptDefinition get() = ScriptDefinitionProperties

@Suppress("MemberVisibilityCanBePrivate")
class RefineConfigurationBuilder(props: ScriptingProperties) : PropertiesBuilder(props) {

    inline operator fun invoke(body: RefineConfigurationBuilder.() -> Unit) {
        body()
    }

    fun handler(fn: RefineScriptCompilationConfigurationHandler) {
        props.data[ScriptDefinitionProperties.refineConfigurationHandler] = fn
    }

    fun beforeParsing(value: Boolean = true) {
        props.data[ScriptDefinitionProperties.refineConfigurationBeforeParsing] = value
    }

    fun onAnnotations(annotations: Iterable<KotlinType>) {
        props.data.addToListProperty(ScriptDefinitionProperties.refineConfigurationOnAnnotations, annotations)
    }

    fun onAnnotations(vararg annotations: KotlinType) {
        onAnnotations(annotations.asIterable())
    }

    inline fun <reified T : Annotation> onAnnotations() {
        onAnnotations(KotlinType(T::class))
    }

    fun onAnnotations(vararg annotations: KClass<out Annotation>) {
        onAnnotations(annotations.map { KotlinType(it) })
    }

    fun onSections(sections: Iterable<String>) {
        props.data.addToListProperty(ScriptDefinitionProperties.refineConfigurationOnSections, sections)
    }

    fun onSections(vararg sections: String) {
        props.data.addToListProperty(ScriptDefinitionProperties.refineConfigurationOnSections, *sections)
    }
}

