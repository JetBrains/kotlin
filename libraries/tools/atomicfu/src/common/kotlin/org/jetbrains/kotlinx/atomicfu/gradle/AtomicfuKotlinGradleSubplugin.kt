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

package org.jetbrains.kotlinx.atomicfu.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

private const val EXTENSION_NAME = "atomicfuCompilerPlugin"

class AtomicfuKotlinGradleSubplugin :
    KotlinCompilerPluginSupportPlugin {
    companion object {
        const val ATOMICFU_ARTIFACT_NAME = "kotlin-atomicfu-compiler-plugin-embeddable"
    }

    override fun apply(target: Project) {
        target.extensions.create(EXTENSION_NAME, AtomicfuKotlinGradleExtension::class.java)
        super.apply(target)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        val config = project.extensions.getByType(AtomicfuKotlinGradleExtension::class.java)
        return (config.isJsIrTransformationEnabled && kotlinCompilation.target.isJs()) ||
                (config.isJvmIrTransformationEnabled && kotlinCompilation.target.isJvm()) ||
                (config.isNativeIrTransformationEnabled && kotlinCompilation.target.isNative())
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }

    open class AtomicfuKotlinGradleExtension {
        var isJsIrTransformationEnabled = false
        var isJvmIrTransformationEnabled = false
        var isNativeIrTransformationEnabled = false
    }

    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(ATOMICFU_ARTIFACT_NAME)

    override fun getCompilerPluginId() = "org.jetbrains.kotlinx.atomicfu"

    private fun KotlinTarget.isJs() = platformType == KotlinPlatformType.js

    private fun KotlinTarget.isJvm() = platformType == KotlinPlatformType.jvm || platformType == KotlinPlatformType.androidJvm

    private fun KotlinTarget.isNative() = platformType == KotlinPlatformType.native // todo wasm?
}
