/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.configuration

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource
import java.io.File

object ScriptingConfigurationKeys {

    val SCRIPT_DEFINITIONS = CompilerConfigurationKey.create<List<ScriptDefinition>>("script definitions")

    val SCRIPT_DEFINITIONS_SOURCES =
        CompilerConfigurationKey.create<List<ScriptDefinitionsSource>>("script definitions sources")

    val DISABLE_SCRIPTING_PLUGIN_OPTION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("Disable scripting plugin")

    val SCRIPT_DEFINITIONS_CLASSES: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("Script definition classes")

    val SCRIPT_DEFINITIONS_CLASSPATH: CompilerConfigurationKey<List<File>> =
        CompilerConfigurationKey.create("Additional classpath for the script definitions")

    val DISABLE_SCRIPT_DEFINITIONS_FROM_CLASSPATH_OPTION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("Do not extract script definitions from the compilation classpath")

    val LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION: CompilerConfigurationKey<MutableMap<String, Any?>> =
        CompilerConfigurationKey.create("Script resolver environment")
}