/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.internal

import kotlinx.serialization.json.JsonPrimitive

internal fun String.jsQuoted(): String = JsonPrimitive(this).toString()
