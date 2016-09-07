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

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.model.SourceProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.annotation.SourceAnnotationsRegistry
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

// apply plugin: 'kotlin-kapt2'
class Kapt2GradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(Kapt2GradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {}
}

// Subplugin for the Kotlin Gradle plugin
class Kapt2KotlinGradleSubplugin : KotlinGradleSubplugin<KotlinCompile> {
    companion object {
        private val VERBOSE_OPTION_NAME = "kapt.verbose"

        fun getKaptConfigurationName(sourceSetName: String): String {
            return if (sourceSetName != "main") "kapt${sourceSetName.capitalize()}" else "kapt"
        }
    }
    
    override fun isApplicable(project: Project, task: KotlinCompile) = Kapt2GradleSubplugin.isEnabled(project)

    fun getKaptGeneratedDir(project: Project, sourceSetName: String): File {
        return File(project.project.buildDir, "generated/source/kapt2/$sourceSetName")
    }

    private fun Project.findKaptConfiguration(sourceSetName: String): Configuration? {
        return project.configurations.findByName(getKaptConfigurationName(sourceSetName))
    }

    override fun apply(
            project: Project, 
            kotlinCompile: KotlinCompile,
            javaCompile: AbstractCompile,
            variantData: Any?,
            javaSourceSet: SourceSet?
    ): List<SubpluginOption> {
        assert((variantData != null) xor (javaSourceSet != null))

        val pluginOptions = mutableListOf<SubpluginOption>()
        val kaptClasspath = arrayListOf<File>()

        fun handleSourceSet(sourceSetName: String) {
            val kapt2Configuration = project.findKaptConfiguration(sourceSetName)
            if (kapt2Configuration != null && kapt2Configuration.dependencies.size > 1) {
                javaCompile.dependsOn(kapt2Configuration.buildDependencies)
                kaptClasspath.addAll(kapt2Configuration.resolve())
            }
        }
        
        val sourceSetName = if (variantData != null) {
            for (provider in (variantData as BaseVariantData<*>).sourceProviders) {
                handleSourceSet((provider as AndroidSourceSet).name)
            }

            variantData.name
        }
        else {
            if (javaSourceSet == null) error("Java source set should not be null")

            handleSourceSet(javaSourceSet.name)
            javaSourceSet.name
        }

        val generatedFilesDir = getKaptGeneratedDir(project, sourceSetName)
        if (variantData != null) {
            (variantData as BaseVariantData<*>).addJavaSourceFoldersToModel(generatedFilesDir)
        }

        // Skip annotation processing in kotlinc if no kapt dependencies were provided
        if (kaptClasspath.isEmpty()) return emptyList()
        kaptClasspath.forEach { pluginOptions += SubpluginOption("apclasspath", it.absolutePath) }
        
        javaCompile.source(generatedFilesDir)
        
        // Completely disable annotation processing in Java
        (javaCompile as? JavaCompile)?.let { javaCompile ->
            val options = javaCompile.options
            options.compilerArgs = options.compilerArgs.filter { !it.startsWith("-proc:") } + "-proc:none"
        }

        pluginOptions += SubpluginOption("generated", generatedFilesDir.canonicalPath)
        pluginOptions += SubpluginOption("classes", kotlinCompile.destinationDir.canonicalPath)

        val kaptExtension = project.extensions.getByType(KaptExtension::class.java)
        if (kaptExtension.generateStubs) {
            project.logger.warn("'kapt.generateStubs' is not used by the 'kotlin-kapt' plugin")
        }
        
        if (project.hasProperty(VERBOSE_OPTION_NAME) && project.property(VERBOSE_OPTION_NAME) == "true") {
            pluginOptions += SubpluginOption("verbose", "true")
        }

        val annotationsFile = File(kotlinCompile.taskBuildDirectory, "source-annotations.txt")
        kotlinCompile.sourceAnnotationsRegistry = SourceAnnotationsRegistry(annotationsFile)

        val incrementalCompilationDataFile = File(project.buildDir, "tmp/kapt2/$sourceSetName/incrementalData.txt")
        pluginOptions += SubpluginOption("incrementalData", incrementalCompilationDataFile.absolutePath)
        
        return pluginOptions
    }

    private val BaseVariantData<*>.sourceProviders: List<SourceProvider>
        get() = variantConfiguration.sortedSourceProviders

    override fun getPluginName() = "org.jetbrains.kotlin.kapt2"
    override fun getGroupName() = "org.jetbrains.kotlin"
    override fun getArtifactName() = "kotlin-annotation-processing"
}