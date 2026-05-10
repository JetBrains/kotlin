/*
 * Copyright 2023-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2020-2023 Brian Norman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jetbrains.kotlin.powerassert.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.property
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import javax.inject.Inject

@ExperimentalKotlinGradlePluginApi
abstract class PowerAssertGradleExtension @Inject constructor(
    objectFactory: ObjectFactory,
) {
    /**
     * Enables (`true`) or disables (`false`) adding the Power-Assert runtime library as a dependency. Defaults to `true`.
     * When enabled, the runtime library is automatically added as an `implementation` dependency to [compilations][compilationFilter] the
     * compiler plugin will transform.
     */
    val addRuntimeDependency: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)

    /**
     * Defines the fully-qualified path of functions which should be transformed by the Power-Assert compiler plugin.
     * If nothing is defined, defaults to [`kotlin.assert`][assert].
     */
    val functions: SetProperty<String> =
        objectFactory.setProperty(String::class.java).convention(setOf("kotlin.assert"))

    /**
     * Defines the Kotlin SourceSets by name which will be transformed by the Power-Assert compiler plugin.
     * When the provider returns `null` or an empty Set (the default) the value of [compilationFilter] will be used.
     */
    @Deprecated(
        "It is recommended to use 'compilationFilter' instead, with one of the available preset filters.",
        level = DeprecationLevel.WARNING,
    )
    val includedSourceSets: SetProperty<String> =
        objectFactory.setProperty(String::class.java).convention(emptySet())

    /**
     * Filter applied to [KotlinCompilation]s to determine which should be transformed by the Power-Assert compiler plugin.
     * Defaults to transforming [PowerAssertCompilationFilter.TESTS] compilations when the provider returns `null` or is unchanged.
     *
     * ```kotlin
     * powerAssert {
     *     compilationFilter.set({
     *         it.name == KotlinCompilation.TEST_COMPILATION_NAME
     *     })
     * }
     * ```
     */
    val compilationFilter: Property<PowerAssertCompilationFilter> =
        objectFactory.property<PowerAssertCompilationFilter>().convention(PowerAssertCompilationFilter.TESTS)
}
