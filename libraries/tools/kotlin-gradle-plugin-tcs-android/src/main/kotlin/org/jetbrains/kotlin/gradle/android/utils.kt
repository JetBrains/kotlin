/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.android

fun camelCase(vararg parts: String): String {
    if (parts.isEmpty()) return ""
    return parts.joinToString("") { it.capitalize() }.decapitalize()
}
