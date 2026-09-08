/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.debugger

import java.net.URLDecoder
import java.net.URLEncoder

internal fun encodeUrlComponent(value: String): String = URLEncoder.encode(value, "UTF-8")

internal fun decodeUrlComponent(value: String): String = URLDecoder.decode(value, "UTF-8")
