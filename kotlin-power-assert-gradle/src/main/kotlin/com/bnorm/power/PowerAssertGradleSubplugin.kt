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

import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinGradleSubplugin::class)
class PowerAssertGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {

  override fun getCompilerPluginId(): String = "com.bnorm.kotlin-power-assert"

  override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
    project.plugins.hasPlugin(PowerAssertGradlePlugin::class.java)


  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.bnorm.power",
    artifactId = "kotlin-power-assert",
    version = "0.3.1"
  )

  override fun apply(
    project: Project,
    kotlinCompile: AbstractCompile,
    javaCompile: AbstractCompile?,
    variantData: Any?,
    androidProjectHandler: Any?,
    kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
  ): List<SubpluginOption> {
    val extension = project.extensions.findByType(PowerAssertGradleExtension::class.java)
      ?: PowerAssertGradleExtension()

    return extension.functions.map {
      SubpluginOption(key = "function", value = it)
    }
  }
}
