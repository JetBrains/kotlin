/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.internal

import kotlinx.serialization.json.JsonPrimitive

/** Quotes for embedding in generated JS. Also escapes U+2028/U+2029, as Gson did. */
internal fun String.jsQuoted(): String = JsonPrimitive(this).toString()
    .replace("\u2028", "\\u2028")
    .replace("\u2029", "\\u2029")
