/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.ide_services.compiler

import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

@Deprecated("This declaration would be removed in future versions", level = DeprecationLevel.WARNING)
interface ReplCompletionOptionsKeys

@Deprecated("This declaration would be removed in future versions", level = DeprecationLevel.WARNING)
open class ReplCompletionOptionsBuilder : PropertiesCollection.Builder(), ReplCompletionOptionsKeys {
    companion object : ReplCompletionOptionsKeys
}

@Deprecated("This declaration would be removed in future versions", level = DeprecationLevel.WARNING)
fun ReplCompletionOptionsBuilder.filterOutShadowedDescriptors(value: Boolean) {
    this[filterOutShadowedDescriptors] = value
}

@Deprecated("This declaration would be removed in future versions", level = DeprecationLevel.WARNING)
fun ReplCompletionOptionsBuilder.nameFilter(value: (String, String) -> Boolean) {
    this[nameFilter] = value
}

@Deprecated("This declaration would be removed in future versions", level = DeprecationLevel.WARNING)
val ReplCompletionOptionsKeys.filterOutShadowedDescriptors by PropertiesCollection.key(true)

@Deprecated("This declaration would be removed in future versions", level = DeprecationLevel.WARNING)
val ReplCompletionOptionsKeys.nameFilter
        by PropertiesCollection.key<(String, String) -> Boolean>({ name, namePart -> name.startsWith(namePart) })

@Suppress("unused")
@Deprecated("This declaration would be removed in future versions", level = DeprecationLevel.WARNING)
val ScriptCompilationConfigurationKeys.completion
    get() = ReplCompletionOptionsBuilder()
