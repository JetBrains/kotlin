/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

/**
 * Indicates if the tests are currently executed on CI like teamcity
 */
val isCI = System.getProperty("is.ci")?.toBoolean() ?: throw RuntimeException("Missing 'is.ci' System property")