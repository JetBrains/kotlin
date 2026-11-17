/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.junit.jupiter.api.parallel.ResourceLock

/**
 * Can be used to mark a test as 'Uses Gradle', using a [ResourceLock] under the hood.
 * Only one test can be executed at a time, which uses Gradle.
 */
@ResourceLock("Gradle")
internal annotation class GradleLock
