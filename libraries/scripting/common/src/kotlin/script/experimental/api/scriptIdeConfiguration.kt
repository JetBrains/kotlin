/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
    Project,     // Project infrastructure: project files excluding source roots
    Everywhere;  // All places in the project
}

val ScriptCompilationConfigurationKeys.ide
    get() = IdeScriptCompilationConfigurationBuilder()

val IdeScriptCompilationConfigurationKeys.dependenciesSources by PropertiesCollection.key<List<ScriptDependency>>()

val IdeScriptCompilationConfigurationKeys.acceptedLocations
        by PropertiesCollection.key(listOf(ScriptAcceptedLocation.Everywhere))
