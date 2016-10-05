/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.noarg.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinJpaSubplugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(NoArgGradleSubplugin::class.java)
        val noArgExtension = NoArgGradleSubplugin.getNoArgExtension(project)
        noArgExtension.annotation("javax.persistence.Entity")
    }
}