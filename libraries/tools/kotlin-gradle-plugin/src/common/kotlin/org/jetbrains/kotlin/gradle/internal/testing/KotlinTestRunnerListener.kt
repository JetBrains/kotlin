/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing

@Deprecated("No longer used. Scheduled for removal in Kotlin 2.6.20.")
interface KotlinTestRunnerListener {
    fun runningFailure(failure: Error)
}
