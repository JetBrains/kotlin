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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.dsl.*

class AtomicfuGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(AtomicfuGradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {
        // nothing here
    }
}

class AtomicfuKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val ATOMICFU_ARTIFACT_NAME = "atomicfu"
    }

    override fun isApplicable(project: Project, task: AbstractCompile) =
        task is Kotlin2JsCompile && project.hasIrTargets()


    private fun Project.hasIrTargets(): Boolean {
        extensions.findByType(KotlinProjectExtension::class.java)?.let { kotlinExtension ->
            if (kotlinExtension is KotlinJsProjectExtension) {
                if (kotlinExtension._target?.isJsIrTarget() == true) return true
            }
            val targetsExtension = (kotlinExtension as? ExtensionAware)?.extensions?.findByName("targets")
            @Suppress("UNCHECKED_CAST")
            if (targetsExtension != null) {
                val targets = targetsExtension as NamedDomainObjectContainer<KotlinTarget>
                if (targets.any { it.isJsIrTarget() }) return true
            }
        }
        return false
    }

    private fun KotlinTarget.isJsIrTarget(): Boolean =
        (this is KotlinJsTarget && this.irTarget != null) || this is KotlinJsIrTarget

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {
        return emptyList()
    }

    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(ATOMICFU_ARTIFACT_NAME)

    override fun getCompilerPluginId() = "org.jetbrains.kotlinx.atomicfu"
}
