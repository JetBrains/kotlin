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

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

class PowerAssertGradlePlugin : KotlinCompilerPluginSupportPlugin {
    companion object {
        private const val POWER_ASSERT_ARTIFACT_NAME = "kotlin-power-assert-compiler-plugin-embeddable"

        private const val FUNCTION_ARG_NAME = "function"
    }

    override fun apply(target: Project) {
        target.extensions.create("powerAssert", PowerAssertGradleExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PowerAssertGradleExtension::class.java)
        return extension.excludedSourceSets.none { it == kotlinCompilation.defaultSourceSet.name }
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PowerAssertGradleExtension::class.java)
        return project.provider {
            extension.functions.map {
                SubpluginOption(key = FUNCTION_ARG_NAME, value = it)
            }
        }
    }

    override fun getCompilerPluginId(): String = "org.jetbrains.kotlin.powerassert"
    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(POWER_ASSERT_ARTIFACT_NAME)
}
