/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.reflect.KClass

object ScriptCompileConfigurationParams {

    val scriptSourceFragments by typedKey<ScriptSourceFragments>()

    val scriptSignature by typedKey<ScriptSignature>()

    val importedPackages by typedKey<Iterable<String>>()

    val restrictions by typedKey<ResolvingRestrictions>()

    val importedScripts by typedKey<Iterable<ScriptSource>>()

    val dependencies by typedKey<Iterable<ScriptDependency>>()

    val compilerOptions by typedKey<Iterable<String>>() // Q: CommonCompilerOptions instead?

    val updateConfigurationOnAnnotations by typedKey<Iterable<KClass<out Annotation>>>()

    val updateConfigurationOnSections by typedKey<Iterable<String>>()
}

typealias ScriptCompileConfiguration = HeterogeneousMap

fun ScriptSource.toScriptCompileConfiguration(vararg pairs: Pair<TypedKey<*>, Any?>) =
    ScriptCompileConfiguration(ScriptCompileConfigurationParams.scriptSourceFragments to ScriptSourceFragments(this, null), *pairs)

object ProcessedScriptDataParams {
    val annotations by typedKey<Iterable<Annotation>>()

    val fragments by typedKey<Iterable<ScriptSourceNamedFragment>>()
}

typealias ProcessedScriptData = HeterogeneousMap


interface ScriptConfigurator {

    // with null scriptSource should return a generic configuration for the script type
    suspend fun baseConfiguration(scriptSource: ScriptSource?) : ResultWithDiagnostics<ScriptCompileConfiguration>

    suspend fun refineConfiguration(
        configuration: ScriptCompileConfiguration,
        processedScriptData: ProcessedScriptData = ProcessedScriptData()
    ): ResultWithDiagnostics<ScriptCompileConfiguration>
}

fun ScriptSource?.toConfigEntry(): Pair<TypedKey<*>, Any?> =
    ScriptCompileConfigurationParams.scriptSourceFragments to this?.let { ScriptSourceFragments(this, null) }

