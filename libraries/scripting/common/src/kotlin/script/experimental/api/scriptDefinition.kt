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

val ScriptDefinitionKeys.displayName by PropertiesCollection.key<String>("Kotlin script") // Name of the script type

val ScriptDefinitionKeys.fileExtension by PropertiesCollection.key<String>("kts") // file extension

val ScriptDefinitionKeys.baseClass by PropertiesCollection.key<KotlinType>() // script base class

val ScriptDefinitionKeys.scriptBodyTarget by PropertiesCollection.key<ScriptBodyTarget>(ScriptBodyTarget.Constructor)

val ScriptDefinitionKeys.implicitReceivers by PropertiesCollection.key<List<KotlinType>>() // in the order from outer to inner scope

val ScriptDefinitionKeys.providedProperties by PropertiesCollection.key<Map<String, KotlinType>>() // external variables

val ScriptDefinitionKeys.defaultImports by PropertiesCollection.key<List<String>>()

val ScriptDefinitionKeys.dependencies by PropertiesCollection.key<List<ScriptDependency>>()

val ScriptDefinitionKeys.generatedClassAnnotations by PropertiesCollection.key<List<Annotation>>()

val ScriptDefinitionKeys.copyAnnotationsFrom by PropertiesCollection.key<List<KotlinType>>()

val ScriptDefinitionKeys.compilerOptions by PropertiesCollection.key<List<String>>() // Q: CommonCompilerOptions instead?

val ScriptDefinitionKeys.refineConfigurationBeforeParsing by PropertiesCollection.key<RefineScriptCompilationConfigurationHandler>() // default: false

val ScriptDefinitionKeys.refineConfigurationOnAnnotations by PropertiesCollection.key<RefineConfigurationOnAnnotationsData>()

val ScriptDefinitionKeys.refineConfigurationOnSections by PropertiesCollection.key<RefineConfigurationOnSectionsData>()

// DSL:

val ScriptDefinition.Builder.refineConfiguration get() = RefineConfigurationBuilder()


class RefineConfigurationBuilder : PropertiesCollection.Builder() {

    fun beforeParsing(handler: RefineScriptCompilationConfigurationHandler) {
        set(ScriptDefinition.refineConfigurationBeforeParsing, handler)
    }

    fun onAnnotations(annotations: List<KotlinType>, handler: RefineScriptCompilationConfigurationHandler) {
        set(ScriptDefinition.refineConfigurationOnAnnotations, RefineConfigurationOnAnnotationsData(annotations, handler))
    }

    fun onAnnotations(vararg annotations: KotlinType, handler: RefineScriptCompilationConfigurationHandler) {
        onAnnotations(annotations.asList(), handler)
    }

    inline fun <reified T : Annotation> onAnnotations(noinline handler: RefineScriptCompilationConfigurationHandler) {
        onAnnotations(listOf(KotlinType(T::class)), handler)
    }

    fun onAnnotations(vararg annotations: KClass<out Annotation>, handler: RefineScriptCompilationConfigurationHandler) {
        onAnnotations(annotations.map { KotlinType(it) }, handler)
    }

    fun onAnnotations(annotations: Iterable<KClass<out Annotation>>, handler: RefineScriptCompilationConfigurationHandler) {
        onAnnotations(annotations.map { KotlinType(it) }, handler)
    }

    fun onSections(sections: List<String>, handler: RefineScriptCompilationConfigurationHandler) {
        set(ScriptDefinition.refineConfigurationOnSections, RefineConfigurationOnSectionsData(sections, handler))
    }

    fun onSections(vararg sections: String, handler: RefineScriptCompilationConfigurationHandler) {
        onSections(sections.asList(), handler)
    }
}

