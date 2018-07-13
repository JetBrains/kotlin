/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.ChainedPropertyBag


typealias ScriptCompileConfiguration = ChainedPropertyBag

typealias ProcessedScriptData = ChainedPropertyBag


interface ScriptCompilationConfigurator {

    // constructor(properties: ScriptDefinitionPropertiesBag) // the constructor is expected from implementations

    val defaultConfiguration: ScriptCompileConfiguration

    suspend fun refineConfiguration(
        scriptSource: ScriptSource,
        configuration: ScriptCompileConfiguration,
        processedScriptData: ProcessedScriptData = ProcessedScriptData()
    ): ResultWithDiagnostics<ScriptCompileConfiguration> =
        configuration.cloneWithNewParent(defaultConfiguration).asSuccess()
}

