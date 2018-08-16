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

val ScriptCompileConfigurationKeys.scriptImplicitReceivers by PropertiesCollection.keyCopy(ScriptDefinition.scriptImplicitReceivers)

val ScriptCompileConfigurationKeys.contextVariables by PropertiesCollection.keyCopy(ScriptDefinition.contextVariables)

val ScriptCompileConfigurationKeys.defaultImports by PropertiesCollection.keyCopy(ScriptDefinition.defaultImports)

val ScriptCompileConfigurationKeys.restrictions by PropertiesCollection.keyCopy(ScriptDefinition.restrictions)

val ScriptCompileConfigurationKeys.importedScripts by PropertiesCollection.keyCopy(ScriptDefinition.importedScripts)

val ScriptCompileConfigurationKeys.dependencies by PropertiesCollection.keyCopy(ScriptDefinition.dependencies)

val ScriptCompileConfigurationKeys.compilerOptions by PropertiesCollection.keyCopy(ScriptDefinition.compilerOptions)


interface ProcessedScriptDataKeys

class ProcessedScriptData(properties: Map<PropertiesCollection.Key<*>, Any>) : PropertiesCollection(properties) {

    companion object : ProcessedScriptDataKeys
}

val ProcessedScriptDataKeys.foundAnnotations by PropertiesCollection.key<List<Annotation>>()

val ProcessedScriptDataKeys.foundFragments by PropertiesCollection.key<List<ScriptSourceNamedFragment>>()


interface RefineScriptCompilationConfigurationHandler {
    suspend operator fun invoke(
        scriptSource: ScriptSource,
        scriptDefinition: ScriptDefinition,
        configuration: ScriptCompileConfiguration?,
        processedScriptData: ProcessedScriptData? = null
    ): ResultWithDiagnostics<ScriptCompileConfiguration?>
}
