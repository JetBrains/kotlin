/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler

import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

interface ReplCompletionOptionsKeys

open class ReplCompletionOptionsBuilder : PropertiesCollection.Builder(), ReplCompletionOptionsKeys {
    companion object : ReplCompletionOptionsKeys
}

fun ReplCompletionOptionsBuilder.filterOutShadowedDescriptors(value: Boolean) {
    this[filterOutShadowedDescriptors] = value
}

fun ReplCompletionOptionsBuilder.nameFilter(value: (String, String) -> Boolean) {
    this[nameFilter] = value
}

val ReplCompletionOptionsKeys.filterOutShadowedDescriptors by PropertiesCollection.key(true)
val ReplCompletionOptionsKeys.nameFilter
        by PropertiesCollection.key<(String, String) -> Boolean>({ name, namePart -> name.startsWith(namePart) })

@Suppress("unused")
val ScriptCompilationConfigurationKeys.completion
    get() = ReplCompletionOptionsBuilder()
