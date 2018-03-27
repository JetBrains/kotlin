/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.reflect.KClass

object ScriptCompileConfigurationParams {

    val baseClass by typedKey<KClass<*>>()

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

