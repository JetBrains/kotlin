/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.plugin.JsMode

fun jsMode(mode: JsMode) =
    "\n${JsMode.jsModeProp}=$mode"