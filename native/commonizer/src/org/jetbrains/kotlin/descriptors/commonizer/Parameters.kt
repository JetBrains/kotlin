/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

class Parameters(
    val statsCollector: StatsCollector? = null
) {
    // use linked hash map to preserve order
    private val _targetProviders = LinkedHashMap<InputTarget, TargetProvider>()

    val targetProviders: List<TargetProvider> get() = _targetProviders.values.toList()

    fun addTarget(targetProvider: TargetProvider): Parameters {
        require(targetProvider.target !in _targetProviders) { "Target ${targetProvider.target} is already added" }
        _targetProviders[targetProvider.target] = targetProvider

        return this
    }

    fun hasAnythingToCommonize(): Boolean = _targetProviders.size >= 2
}
