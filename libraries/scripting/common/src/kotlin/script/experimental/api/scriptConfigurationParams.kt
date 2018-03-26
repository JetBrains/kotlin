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

    open class Builder : HeterogeneousMapBuilder() {
        inline fun <reified T> signature(providedDeclarations: ProvidedDeclarations = ProvidedDeclarations.Empty) {
            add(scriptSignature to ScriptSignature(T::class, providedDeclarations))
        }
    }
}

// DSL
inline fun scriptConfiguration(from: HeterogeneousMap = HeterogeneousMap(), body: ScriptCompileConfigurationParams.Builder.() -> Unit) =
    ScriptCompileConfigurationParams.Builder().build(from, body)


fun ScriptSource.toScriptCompileConfiguration(vararg pairs: Pair<TypedKey<*>, Any?>) =
    ScriptCompileConfiguration(ScriptCompileConfigurationParams.scriptSourceFragments to ScriptSourceFragments(this, null), *pairs)

fun ScriptSource?.toConfigEntry(): Pair<TypedKey<*>, Any?> =
    ScriptCompileConfigurationParams.scriptSourceFragments to this?.let { ScriptSourceFragments(this, null) }

