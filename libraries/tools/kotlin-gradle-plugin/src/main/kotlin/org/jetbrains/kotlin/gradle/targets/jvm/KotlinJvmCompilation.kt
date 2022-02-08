 /*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

open class KotlinJvmCompilation(
    override val target: KotlinJvmTarget,
    name: String
) : AbstractKotlinCompilationToRunnableFiles<KotlinJvmOptions>(target, name), KotlinCompilationWithResources<KotlinJvmOptions> {

    override val kotlinOptions: KotlinJvmOptions = KotlinJvmOptionsImpl()

    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    @Suppress("UNCHECKED_CAST")
    override val compileKotlinTaskProvider: TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>
        get() = super.compileKotlinTaskProvider as TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>

    override val compileKotlinTask: org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        get() = super.compileKotlinTask as org.jetbrains.kotlin.gradle.tasks.KotlinCompile

    val compileJavaTaskProvider: TaskProvider<out JavaCompile>?
        get() = if (target.withJavaEnabled) {
            val project = target.project
            val javaSourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
            val javaSourceSet = javaSourceSets.getByName(compilationPurpose)
            project.tasks.withType(JavaCompile::class.java).named(javaSourceSet.compileJavaTaskName)
        } else null
}