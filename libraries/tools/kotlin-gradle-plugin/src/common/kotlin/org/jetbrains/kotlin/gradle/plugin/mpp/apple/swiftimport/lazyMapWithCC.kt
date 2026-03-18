/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Delay execution of the TaskProvider.map to execution with CC
 */
internal fun <T> TaskProvider<*>.lazyMapWithCC(map: () -> T): Provider<T> = flatMap {
    it.outputs.files.elements
}.map {
    map()
}