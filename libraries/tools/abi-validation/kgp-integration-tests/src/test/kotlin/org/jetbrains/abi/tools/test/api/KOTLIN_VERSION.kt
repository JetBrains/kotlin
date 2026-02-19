/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.api

// constant is held in separate file intentionally for better discoverability
// and to prevent vcs conflicts (its value is 1.1-* in master branch, 0.1-* in 1.0.x branches)
val KOTLIN_VERSION get() = System.getProperty("kotlinVersion") ?: error("Required to specify kotlinVersion system property for tests")