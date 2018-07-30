/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.PropertiesCollection

interface ScriptCompileConfiguration : PropertiesCollection {
    
    companion object : ScriptCompileConfiguration {

        class Builder internal constructor() : PropertiesCollection.Builder(), ScriptCompileConfiguration {
            override val properties = data
        }

        fun create(body: Builder.() -> Unit): ScriptCompileConfiguration = Builder().apply(body)
    }
}

val ScriptCompileConfiguration.sourceFragments by PropertiesCollection.key<List<ScriptSourceNamedFragment>>()

val ScriptCompileConfiguration.scriptBodyTarget by PropertiesCollection.keyCopy(ScriptDefinition.scriptBodyTarget)

val ScriptCompileConfiguration.scriptImplicitReceivers by PropertiesCollection.keyCopy(ScriptDefinition.scriptImplicitReceivers)

val ScriptCompileConfiguration.contextVariables by PropertiesCollection.keyCopy(ScriptDefinition.contextVariables)

val ScriptCompileConfiguration.defaultImports by PropertiesCollection.keyCopy(ScriptDefinition.defaultImports)

val ScriptCompileConfiguration.restrictions by PropertiesCollection.keyCopy(ScriptDefinition.restrictions)

val ScriptCompileConfiguration.importedScripts by PropertiesCollection.keyCopy(ScriptDefinition.importedScripts)

val ScriptCompileConfiguration.dependencies by PropertiesCollection.keyCopy(ScriptDefinition.dependencies)

val ScriptCompileConfiguration.compilerOptions by PropertiesCollection.keyCopy(ScriptDefinition.compilerOptions)


interface ProcessedScriptData : PropertiesCollection {

    companion object : ProcessedScriptData
}

val ProcessedScriptData.foundAnnotations by PropertiesCollection.key<List<Annotation>>()

val ProcessedScriptData.foundFragments by PropertiesCollection.key<List<ScriptSourceNamedFragment>>()


interface RefineScriptCompilationConfigurationHandler {
    suspend operator fun invoke(
        scriptSource: ScriptSource,
        scriptDefinition: ScriptDefinition,
        configuration: ScriptCompileConfiguration?,
        processedScriptData: ProcessedScriptData? = null
    ): ResultWithDiagnostics<ScriptCompileConfiguration?>
}
