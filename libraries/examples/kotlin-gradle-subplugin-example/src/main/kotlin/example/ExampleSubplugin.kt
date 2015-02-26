/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package example

import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class ExampleSubplugin : KotlinGradleSubplugin {

    override fun getExtraArguments(project: Project, task: AbstractCompile): List<SubpluginOption>? {
        println("ExampleSubplugin loaded")
        return listOf(SubpluginOption("exampleKey", "exampleValue"))
    }

    override fun getPluginName(): String {
        return "example.plugin"
    }

    override fun getGroupName(): String {
        return "org.jetbrains.kotlin"
    }

    override fun getArtifactName(): String {
        return "kotlin-gradle-subplugin-example"
    }
}