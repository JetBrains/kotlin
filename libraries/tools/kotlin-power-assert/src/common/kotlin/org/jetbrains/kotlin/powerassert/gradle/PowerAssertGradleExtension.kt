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
import org.gradle.api.provider.SetProperty
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import javax.inject.Inject

@ExperimentalKotlinGradlePluginApi
abstract class PowerAssertGradleExtension @Inject constructor(
    objectFactory: ObjectFactory,
) {
    /**
     * Defines the fully-qualified path of functions which should be transformed by the Power-Assert compiler plugin.
     * If nothing is defined, defaults to [`kotlin.assert`][assert].
     */
    val functions: SetProperty<String> = objectFactory.setProperty(String::class.java).convention(setOf("kotlin.assert"))

    /**
     * Defines the Kotlin SourceSets by name which will be transformed by the Power-Assert compiler plugin.
     * When the provider returns `null` - which is the default - all test SourceSets will be transformed.
     */
    val includedSourceSets: SetProperty<String> = objectFactory.setProperty(String::class.java).convention(emptySet())
}
