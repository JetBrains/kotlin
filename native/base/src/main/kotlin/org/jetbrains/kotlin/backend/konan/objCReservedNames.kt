/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

val objCMacroDefinitions = cMacroDefinitions + setOf("DEBUG")

/**
 * Following class and object names should be handled in a special way to avoid clashing with NSObject class methods.
 *
 * When processing ["alloc", "copy", "mutableCopy", "new", "init"] names the `get` prefix should be added.
 * Other reserved names are mangled by adding `_` suffix.
 */
val reservedObjCClassOrObjectNames = setOf(
    "retain", "release", "autorelease",
    "initialize", "load", "alloc", "new", "class", "superclass",
    "classFallbacksForKeyedArchiver", "classForKeyedUnarchiver",
    "description", "debugDescription", "version", "hash",
    "useStoredAccessor"
) + cKeywords + objCMacroDefinitions
