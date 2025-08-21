/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.abi

import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.dsl.abi.AbiFilterSetSpec
import org.jetbrains.kotlin.gradle.dsl.abi.AbiFiltersSpec
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

/**
 * Implementation for [AbiFiltersSpec].
 */
@ExperimentalAbiValidation
internal abstract class AbiFiltersSpecImpl @Inject constructor(objects: ObjectFactory) : AbiFiltersSpec {
    override val excluded: AbiFilterSetSpec = objects.newInstance<AbiFilterSetSpec>()
    override val included: AbiFilterSetSpec = objects.newInstance<AbiFilterSetSpec>()
}

/**
 * Creates an instance of [AbiFiltersSpecImpl].
 */
internal fun ObjectFactory.AbiFiltersSpecImpl(): AbiFiltersSpecImpl = newInstance<AbiFiltersSpecImpl>()

