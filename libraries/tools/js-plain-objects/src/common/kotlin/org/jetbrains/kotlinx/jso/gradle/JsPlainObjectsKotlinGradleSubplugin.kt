/*
 * Copyright 2010-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.jspo.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

class JsPlainObjectsKotlinGradleSubplugin : KotlinCompilerPluginSupportPlugin {
    companion object {
        const val JS_PLAIN_OBJECTS_ARTIFACT_NAME = "kotlinx-js-plain-objects-compiler-plugin-embeddable"
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        kotlinCompilation.target.isJs()

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        kotlinCompilation.dependencies {
            implementation(kotlin("js-plain-objects"))
        }
        return kotlinCompilation.target.project.provider { emptyList() }
    }

    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(JS_PLAIN_OBJECTS_ARTIFACT_NAME)

    override fun getCompilerPluginId() = "org.jetbrains.kotlinx.js-plain-objects"

    private fun KotlinTarget.isJs() = platformType == KotlinPlatformType.js
}
