/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.script.experimental.util.PropertiesCollection

interface ScriptDefinitionKeys

open class ScriptDefinition(baseDefinitions: Iterable<ScriptDefinition>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseDefinitions).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(vararg baseDefinitions: ScriptDefinition, body: Builder.() -> Unit = {}) : this(baseDefinitions.asIterable(), body)

    class Builder internal constructor(baseDefinitions: Iterable<ScriptDefinition>) :
        ScriptDefinitionKeys,
        PropertiesCollection.Builder(baseDefinitions)

    // inherited from script definition for using as a keys anchor
    companion object : ScriptDefinitionKeys {

        val Default = ScriptDefinition()
    }
}

val ScriptDefinitionKeys.name by PropertiesCollection.key<String>("Kotlin script") // Name of the script type

val ScriptDefinitionKeys.fileExtension by PropertiesCollection.key<String>("kts") // file extension

val ScriptDefinitionKeys.baseClass by PropertiesCollection.key<KotlinType>() // script base class

val ScriptDefinitionKeys.scriptBodyTarget by PropertiesCollection.key<ScriptBodyTarget>(ScriptBodyTarget.Constructor)

val ScriptDefinitionKeys.scriptImplicitReceivers by PropertiesCollection.key<List<KotlinType>>() // in the order from outer to inner scope

val ScriptDefinitionKeys.contextVariables by PropertiesCollection.key<Map<String, KotlinType>>() // external variables

val ScriptDefinitionKeys.defaultImports by PropertiesCollection.key<List<String>>()

val ScriptDefinitionKeys.restrictions by PropertiesCollection.key<List<ResolvingRestrictionRule>>()

val ScriptDefinitionKeys.importedScripts by PropertiesCollection.key<List<ScriptSource>>()

val ScriptDefinitionKeys.dependencies by PropertiesCollection.key<List<ScriptDependency>>()

val ScriptDefinitionKeys.generatedClassAnnotations by PropertiesCollection.key<List<Annotation>>()

val ScriptDefinitionKeys.generatedMethodAnnotations by PropertiesCollection.key<List<Annotation>>()

val ScriptDefinitionKeys.compilerOptions by PropertiesCollection.key<List<String>>() // Q: CommonCompilerOptions instead?

val ScriptDefinitionKeys.refineConfigurationHandler by PropertiesCollection.key<RefineScriptCompilationConfigurationHandler>() // dynamic configurator

val ScriptDefinitionKeys.refineConfigurationBeforeParsing by PropertiesCollection.key<Boolean>() // default: false

val ScriptDefinitionKeys.refineConfigurationOnAnnotations by PropertiesCollection.key<List<KotlinType>>()

val ScriptDefinitionKeys.refineConfigurationOnSections by PropertiesCollection.key<List<String>>()

// DSL:

val ScriptDefinition.Builder.refineConfiguration get() = RefineConfigurationBuilder()


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

