/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import com.google.gson.stream.JsonWriter
import java.io.StringWriter

fun String.jsQuoted(): String = StringWriter().also {
    JsonWriter(it).value(this)
}.toString()
