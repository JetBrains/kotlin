/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.PropertiesCollection


interface ScriptCollectedDataKeys

class ScriptCollectedData(properties: Map<PropertiesCollection.Key<*>, Any>) : PropertiesCollection(properties) {

    companion object : ScriptCollectedDataKeys
}

val ScriptCollectedDataKeys.foundAnnotations by PropertiesCollection.key<List<Annotation>>()

val ScriptCollectedDataKeys.foundFragments by PropertiesCollection.key<List<ScriptSourceNamedFragment>>()

class ScriptDataFacade(
    val source: ScriptSource,
    val definition: ScriptDefinition,
    val collectedData: ScriptCollectedData? = null
)

typealias RefineScriptCompilationConfigurationHandler = (ScriptDataFacade) -> ResultWithDiagnostics<ScriptDefinition?>

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
