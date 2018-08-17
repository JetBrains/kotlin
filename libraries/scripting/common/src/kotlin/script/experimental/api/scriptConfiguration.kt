/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.PropertiesCollection

interface ScriptCompileConfigurationKeys

class ScriptCompileConfiguration(baseConfigurations: Iterable<ScriptCompileConfiguration>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseConfigurations).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseConfigurations: ScriptCompileConfiguration, body: Builder.() -> Unit = {}
    ) : this(baseConfigurations.asIterable(), body)

    class Builder internal constructor(baseConfigurations: Iterable<ScriptCompileConfiguration>) :
        ScriptCompileConfigurationKeys,
        PropertiesCollection.Builder(baseConfigurations)
    
    companion object : ScriptCompileConfigurationKeys
}

val ScriptCompileConfigurationKeys.sourceFragments by PropertiesCollection.key<List<ScriptSourceNamedFragment>>()

val ScriptCompileConfigurationKeys.scriptBodyTarget by PropertiesCollection.keyCopy(ScriptDefinition.scriptBodyTarget)

val ScriptCompileConfigurationKeys.scriptImplicitReceivers by PropertiesCollection.keyCopy(ScriptDefinition.implicitReceivers)

val ScriptCompileConfigurationKeys.providedProperties by PropertiesCollection.keyCopy(ScriptDefinition.providedProperties)

val ScriptCompileConfigurationKeys.defaultImports by PropertiesCollection.keyCopy(ScriptDefinition.defaultImports)

val ScriptCompileConfigurationKeys.dependencies by PropertiesCollection.keyCopy(ScriptDefinition.dependencies)

val ScriptCompileConfigurationKeys.compilerOptions by PropertiesCollection.keyCopy(ScriptDefinition.compilerOptions)


interface ScriptCollectedDataKeys

class ScriptCollectedData(properties: Map<PropertiesCollection.Key<*>, Any>) : PropertiesCollection(properties) {

    companion object : ScriptCollectedDataKeys
}

val ScriptCollectedDataKeys.foundAnnotations by PropertiesCollection.key<List<Annotation>>()

val ScriptCollectedDataKeys.foundFragments by PropertiesCollection.key<List<ScriptSourceNamedFragment>>()

class ScriptDataFacade(
    val source: ScriptSource,
    val definition: ScriptDefinition,
    val configuration: ScriptCompileConfiguration?,
    val collectedData: ScriptCollectedData? = null
)

typealias RefineScriptCompilationConfigurationHandler = (ScriptDataFacade) -> ResultWithDiagnostics<ScriptCompileConfiguration?>

class RefineConfigurationOnAnnotationsData(
    val annotations: List<KotlinType>,
    val handler: RefineScriptCompilationConfigurationHandler
)

class RefineConfigurationOnSectionsData(
    val sections: List<String>,
    val handler: RefineScriptCompilationConfigurationHandler
)
