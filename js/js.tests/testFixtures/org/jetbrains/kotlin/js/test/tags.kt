/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.junit.jupiter.api.Tag

/**
 * Tests marked with this annotation use the ES6 (or ES2015) compilation target, instead of the default ES5.
 */
@Tag("es6")
annotation class JsEs6Test
