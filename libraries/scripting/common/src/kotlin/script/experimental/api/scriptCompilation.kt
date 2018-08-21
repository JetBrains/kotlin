/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.script.experimental.util.PropertiesCollection

interface ScriptCompilationConfigurationKeys

open class ScriptCompilationConfiguration(baseConfigurations: Iterable<ScriptCompilationConfiguration>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseConfigurations).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseConfigurations: ScriptCompilationConfiguration, body: Builder.() -> Unit = {}
    ) : this(baseConfigurations.asIterable(), body)

    class Builder internal constructor(baseConfigurations: Iterable<ScriptCompilationConfiguration>) :
        ScriptCompilationConfigurationKeys,
        PropertiesCollection.Builder(baseConfigurations)

    // inherited from script compilationConfiguration for using as a keys anchor
    companion object : ScriptCompilationConfigurationKeys

    object Default : ScriptCompilationConfiguration()
}

val ScriptCompilationConfigurationKeys.displayName by PropertiesCollection.key<String>("Kotlin script") // Name of the script type

val ScriptCompilationConfigurationKeys.fileExtension by PropertiesCollection.key<String>("kts") // file extension

val ScriptCompilationConfigurationKeys.baseClass by PropertiesCollection.key<KotlinType>() // script base class

val ScriptCompilationConfigurationKeys.scriptBodyTarget by PropertiesCollection.key<ScriptBodyTarget>(ScriptBodyTarget.Constructor)

val ScriptCompilationConfigurationKeys.implicitReceivers by PropertiesCollection.key<List<KotlinType>>() // in the order from outer to inner scope

val ScriptCompilationConfigurationKeys.providedProperties by PropertiesCollection.key<Map<String, KotlinType>>() // external variables

val ScriptCompilationConfigurationKeys.defaultImports by PropertiesCollection.key<List<String>>()

val ScriptCompilationConfigurationKeys.dependencies by PropertiesCollection.key<List<ScriptDependency>>()

val ScriptCompilationConfigurationKeys.copyAnnotationsFrom by PropertiesCollection.key<List<KotlinType>>()

val ScriptCompilationConfigurationKeys.compilerOptions by PropertiesCollection.key<List<String>>() // Q: CommonCompilerOptions instead?

val ScriptCompilationConfigurationKeys.refineConfigurationBeforeParsing by PropertiesCollection.key<RefineConfigurationBeforeParsingData>()

val ScriptCompilationConfigurationKeys.refineConfigurationOnAnnotations by PropertiesCollection.key<RefineConfigurationOnAnnotationsData>()

val ScriptCompilationConfigurationKeys.refineConfigurationOnSections by PropertiesCollection.key<RefineConfigurationOnSectionsData>()

val ScriptCompilationConfigurationKeys.sourceFragments by PropertiesCollection.key<List<ScriptSourceNamedFragment>>()

// DSL:

val ScriptCompilationConfiguration.Builder.refineConfiguration get() = RefineConfigurationBuilder()


class RefineConfigurationBuilder : PropertiesCollection.Builder() {

    fun beforeParsing(handler: RefineScriptCompilationConfigurationHandler) {
        set(ScriptCompilationConfiguration.refineConfigurationBeforeParsing, RefineConfigurationBeforeParsingData(handler))
    }

    fun onAnnotations(annotations: List<KotlinType>, handler: RefineScriptCompilationConfigurationHandler) {
        set(ScriptCompilationConfiguration.refineConfigurationOnAnnotations, RefineConfigurationOnAnnotationsData(annotations, handler))
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
        set(ScriptCompilationConfiguration.refineConfigurationOnSections, RefineConfigurationOnSectionsData(sections, handler))
    }

    fun onSections(vararg sections: String, handler: RefineScriptCompilationConfigurationHandler) {
        onSections(sections.asList(), handler)
    }
}

typealias RefineScriptCompilationConfigurationHandler =
            (ScriptConfigurationRefinementContext) -> ResultWithDiagnostics<ScriptCompilationConfiguration>

// to make it "hasheable" for cashing
class RefineConfigurationBeforeParsingData(
    val handler: RefineScriptCompilationConfigurationHandler
)

class RefineConfigurationOnAnnotationsData(
    val annotations: List<KotlinType>,
    val handler: RefineScriptCompilationConfigurationHandler
)

class RefineConfigurationOnSectionsData(
    val sections: List<String>,
    val handler: RefineScriptCompilationConfigurationHandler
)


interface ScriptCompiler {

    suspend operator fun invoke(
        script: ScriptSource,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>>
}


interface CompiledScript<out ScriptBase : Any> {

    val compilationConfiguration: ScriptCompilationConfiguration

    suspend fun instantiate(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<ScriptBase>
}
