/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.script.experimental.util.PropertiesCollection

interface ScriptDefinition : PropertiesCollection {

    companion object : ScriptDefinition {

        class Builder internal constructor() : PropertiesCollection.Builder(), ScriptDefinition {
            override val properties = data
        }

        fun create(body: Builder.() -> Unit): ScriptDefinition = Builder().apply(body)
    }

    object Default : ScriptDefinition
}

val ScriptDefinition.name by PropertiesCollection.key<String>("Kotlin script") // Name of the script type

val ScriptDefinition.fileExtension by PropertiesCollection.key<String>("kts") // file extension

val ScriptDefinition.baseClass by PropertiesCollection.key<KotlinType>() // script base class

val ScriptDefinition.scriptBodyTarget by PropertiesCollection.key<ScriptBodyTarget>(ScriptBodyTarget.Constructor)

val ScriptDefinition.scriptImplicitReceivers by PropertiesCollection.key<List<KotlinType>>() // in the order from outer to inner scope

val ScriptDefinition.contextVariables by PropertiesCollection.key<Map<String, KotlinType>>() // external variables

val ScriptDefinition.defaultImports by PropertiesCollection.key<List<String>>()

val ScriptDefinition.restrictions by PropertiesCollection.key<List<ResolvingRestrictionRule>>()

val ScriptDefinition.importedScripts by PropertiesCollection.key<List<ScriptSource>>()

val ScriptDefinition.dependencies by PropertiesCollection.key<List<ScriptDependency>>()

val ScriptDefinition.generatedClassAnnotations by PropertiesCollection.key<List<Annotation>>()

val ScriptDefinition.generatedMethodAnnotations by PropertiesCollection.key<List<Annotation>>()

val ScriptDefinition.compilerOptions by PropertiesCollection.key<List<String>>() // Q: CommonCompilerOptions instead?

val ScriptDefinition.refineConfigurationHandler by PropertiesCollection.key<RefineScriptCompilationConfigurationHandler>() // dynamic configurator

val ScriptDefinition.refineConfigurationBeforeParsing by PropertiesCollection.key<Boolean>() // default: false

val ScriptDefinition.refineConfigurationOnAnnotations by PropertiesCollection.key<List<KotlinType>>()

val ScriptDefinition.refineConfigurationOnSections by PropertiesCollection.key<List<String>>()

// DSL:

val ScriptDefinition.refineConfiguration get() = RefineConfigurationBuilder()


class RefineConfigurationBuilder : PropertiesCollection.Builder() {

    fun handler(fn: RefineScriptCompilationConfigurationHandler) {
        set(ScriptDefinition.refineConfigurationHandler, fn)
    }

    fun triggerBeforeParsing(value: Boolean = true) {
        set(ScriptDefinition.refineConfigurationBeforeParsing, value)
    }

    fun triggerOnAnnotations(annotations: Iterable<KotlinType>) {
        ScriptDefinition.refineConfigurationOnAnnotations.append(annotations)
    }

    fun triggerOnAnnotations(vararg annotations: KotlinType) {
        triggerOnAnnotations(annotations.asIterable())
    }

    inline fun <reified T : Annotation> triggerOnAnnotations() {
        triggerOnAnnotations(KotlinType(T::class))
    }

    fun triggerOnAnnotations(vararg annotations: KClass<out Annotation>) {
        triggerOnAnnotations(annotations.map { KotlinType(it) })
    }

    fun triggerOnSections(sections: Iterable<String>) {
        ScriptDefinition.refineConfigurationOnSections.append(sections)
    }

    fun triggerOnSections(vararg sections: String) {
        ScriptDefinition.refineConfigurationOnSections.append(*sections)
    }
}

