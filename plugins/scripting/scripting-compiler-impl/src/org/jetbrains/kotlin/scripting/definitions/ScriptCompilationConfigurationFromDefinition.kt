/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import kotlin.script.dependencies.Environment
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

// WARNING: Do not rename this file because gradle has it as a dependency
val ScriptCompilationConfigurationKeys.annotationsForSamWithReceivers by PropertiesCollection.key<List<KotlinType>>()

val ScriptCompilationConfigurationKeys.platform by PropertiesCollection.key<String>()

val ScriptCompilationConfigurationKeys.asyncDependenciesResolver by PropertiesCollection.key<Boolean>()

val ScriptingHostConfigurationKeys.getEnvironment by PropertiesCollection.key<() -> Environment?>()