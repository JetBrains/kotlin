/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bnorm.power

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class PowerAssertGradlePlugin : KotlinCompilerPluginSupportPlugin {
  override fun apply(target: Project): Unit = with(target) {
    extensions.create("kotlinPowerAssert", PowerAssertGradleExtension::class.java)
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = "com.bnorm.kotlin-power-assert"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.bnorm.power",
    artifactId = "kotlin-power-assert-plugin",
    version = "0.5.0-SNAPSHOT"
  )

  override fun getPluginArtifactForNative(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.bnorm.power",
    artifactId = "kotlin-power-assert-plugin-native",
    version = "0.5.0-SNAPSHOT"
  )

  override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(PowerAssertGradleExtension::class.java)
    return project.provider {
      extension.functions.map {
        SubpluginOption(key = "function", value = it)
      }
    }
  }
}
