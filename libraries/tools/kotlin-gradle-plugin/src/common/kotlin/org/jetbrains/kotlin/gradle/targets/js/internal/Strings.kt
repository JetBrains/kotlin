/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.internal

import kotlinx.serialization.json.JsonPrimitive

/**
 * Quotes the receiver so that it can be embedded as a string literal into the JavaScript this plugin generates
 * (webpack.config.js, karma.conf.js, ...).
 *
 * U+2028 and U+2029 are legal inside a JSON string but terminate a line in JavaScript before ES2019, so they are
 * escaped explicitly: Gson, which this used to be built on, escaped them, while kotlinx-serialization does not.
 */
internal fun String.jsQuoted(): String = JsonPrimitive(this).toString()
    .replace("\u2028", "\\u2028")
    .replace("\u2029", "\\u2029")
