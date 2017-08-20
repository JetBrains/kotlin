/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile

class SubpluginOption(val key: String, val value: String)

interface KotlinGradleSubplugin<in KotlinCompile : AbstractCompile> {
    fun isApplicable(project: Project, task: KotlinCompile): Boolean
    
    fun apply(
            project: Project, 
            kotlinCompile: KotlinCompile,
            javaCompile: AbstractCompile,
            variantData: Any?,
            androidProjectHandler: Any?,
            javaSourceSet: SourceSet?
    ): List<SubpluginOption>

    fun getSubpluginKotlinTasks(
            project: Project,
            kotlinCompile: KotlinCompile
    ): List<AbstractCompile> = emptyList()

    fun getCompilerPluginId(): String
    fun getGroupName(): String
    fun getArtifactName(): String
}
