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

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.model.SourceProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.io.File

// apply plugin: 'kotlin-kapt2'
class Kapt2GradleSubplugin : Plugin<Project> {
    private val kapt2Configurations = mutableMapOf<Project, MutableMap<String, Configuration>>()
    
    fun getConfigurations(project: Project): Map<String, Configuration> = kapt2Configurations[project] ?: emptyMap()

    override fun apply(project: Project) {
        val androidExt = project.extensions.findByName("android") as? BaseExtension
        if (androidExt != null) {
            androidExt.sourceSets.all { sourceSet ->
                addKapt2Configuration(project, sourceSet.name)
            }
        } 
        else {
            val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
            javaPluginConvention.sourceSets.all { sourceSet ->
                addKapt2Configuration(project, sourceSet.name)
            }
        }
    }
    
    private fun addKapt2Configuration(project: Project, sourceSetName: String) {
        val aptConfigurationName = if (sourceSetName != "main") "kapt2${sourceSetName.capitalize()}" else "kapt2"
        val configuration = project.configurations.create(aptConfigurationName)
        kapt2Configurations.getOrPut(project, { mutableMapOf() }).put(sourceSetName, configuration)
    }
}

// Subplugin for the Kotlin Gradle plugin
class Kapt2KotlinGradleSubplugin : KotlinGradleSubplugin {
    private companion object {
        val VERBOSE_OPTION_NAME = "kapt2.verbose"
    }
    
    override fun isApplicable(project: Project, task: AbstractCompile): Boolean {
        return project.plugins.findPlugin(Kapt2GradleSubplugin::class.java) != null
    }

    fun getKaptGeneratedDir(project: Project, sourceSetName: String): File {
        return File(project.project.buildDir, "generated/source/kapt2/$sourceSetName")
    }

    override fun apply(
            project: Project, 
            kotlinCompile: AbstractCompile, 
            javaCompile: AbstractCompile?, 
            variantData: Any?,
            javaSourceSet: SourceSet?
    ): List<SubpluginOption> {
        //assertion only one variantData / javaSourceSet

        // delete
        if (javaCompile == null) error("Java compile task should exist")
        
        val kapt2Configurations = project.plugins.findPlugin(Kapt2GradleSubplugin::class.java).getConfigurations(project)

        val pluginOptions = mutableListOf<SubpluginOption>()
        val kaptClasspath = arrayListOf<File>()
        
        val generatedFilesDir = if (variantData != null) {
            variantData as BaseVariantData<*>
            
            for (provider in variantData.sourceProviders) {
                val kapt2Configuration = kapt2Configurations[(provider as AndroidSourceSet).name]
                // extract & java
                if (kapt2Configuration != null && kapt2Configuration.dependencies.size > 0) {
                    javaCompile.dependsOn(kapt2Configuration.buildDependencies)
                    kaptClasspath.addAll(kapt2Configuration.resolve())
                }
            }

            getKaptGeneratedDir(project, variantData.name).apply { variantData.addJavaSourceFoldersToModel(this) }
        }
        else if (javaSourceSet != null) {
            val kapt2Configuration = kapt2Configurations[javaSourceSet.name]
            if (kapt2Configuration != null && kapt2Configuration.dependencies.size > 0) {
                javaCompile.dependsOn(kapt2Configuration.buildDependencies)
                kaptClasspath.addAll(kapt2Configuration.resolve())
            }

            getKaptGeneratedDir(project, javaSourceSet.name)
        } 
        else {
            throw IllegalArgumentException("variantData or javaSourceSet should be provided")
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
        
        if (project.hasProperty(VERBOSE_OPTION_NAME) && project.property(VERBOSE_OPTION_NAME) == "true") {
            pluginOptions += SubpluginOption("verbose", "true")
        }
        
        return pluginOptions
    }

    private val BaseVariantData<*>.sourceProviders: List<SourceProvider>
        get() = variantConfiguration.sortedSourceProviders

    override fun getPluginName() = "org.jetbrains.kotlin.kapt2"
    override fun getGroupName() = "org.jetbrains.kotlin"
    override fun getArtifactName() = "kotlin-annotation-processing"
}