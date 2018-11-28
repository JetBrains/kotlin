/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.PropertiesCollection

interface IdeScriptCompilationConfigurationKeys

open class IdeScriptCompilationConfigurationBuilder : PropertiesCollection.Builder(),
    IdeScriptCompilationConfigurationKeys {
    companion object : IdeScriptCompilationConfigurationKeys
}

enum class ScriptAcceptedLocation {
    Sources,     // Under sources roots
    Tests,       // Under test sources roots
    Libraries,   // Under libraries classes or sources
    Project,     // Under project folder, including sources and test sources roots
    Everywhere;
}

val ScriptCompilationConfigurationKeys.ide
    get() = IdeScriptCompilationConfigurationBuilder()

val IdeScriptCompilationConfigurationKeys.dependenciesSources by PropertiesCollection.key<List<ScriptDependency>>()

@Suppress("RemoveExplicitTypeArguments")
val IdeScriptCompilationConfigurationKeys.acceptedLocations
        by PropertiesCollection.key<List<ScriptAcceptedLocation>>(
            listOf(
                ScriptAcceptedLocation.Sources,
                ScriptAcceptedLocation.Tests
            )
        )
