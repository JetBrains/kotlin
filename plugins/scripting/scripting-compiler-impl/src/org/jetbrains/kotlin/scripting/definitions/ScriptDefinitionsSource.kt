/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "Using ScriptDefinitionsSource directly is deprecated, replace with `ScriptDefinitionsProvider`. See: KT-82551",
    replaceWith = ReplaceWith("ScriptDefinitionsProvider", "kotlin.script.experimental.intellij")
)
interface ScriptDefinitionsSource {
    val definitions: Sequence<ScriptDefinition>
}