/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.typedKey

object ScriptCompileConfigurationProperties {

    val sourceFragments by typedKey<List<ScriptSourceNamedFragment>>()

    val baseClass = ScriptingEnvironmentProperties.baseClass

    val scriptBodyTarget by typedKey<ScriptBodyTarget>()

    val scriptImplicitReceivers by typedKey<List<KotlinType>>() // in the order from outer to inner scope

    val contextVariables by typedKey<Map<String, KotlinType>>() // external variables

    val defaultImports by typedKey<List<String>>()

    val restrictions by typedKey<List<ResolvingRestrictionRule>>()

    val importedScripts by typedKey<List<ScriptSource>>()

    val dependencies by typedKey<List<ScriptDependency>>()

    val generatedClassAnnotations by typedKey<List<Annotation>>()

    val generatedMethodAnnotations by typedKey<List<Annotation>>()

    val compilerOptions by typedKey<List<String>>() // Q: CommonCompilerOptions instead?

    val refineBeforeParsing by typedKey<Boolean>() // default: false

    val refineConfigurationOnAnnotations by typedKey<List<KotlinType>>()

    val refineConfigurationOnSections by typedKey<List<String>>()
}

