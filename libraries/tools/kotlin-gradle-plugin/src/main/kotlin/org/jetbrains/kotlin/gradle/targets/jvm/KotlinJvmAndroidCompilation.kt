 /*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.getJavaTaskProvider
import org.jetbrains.kotlin.gradle.plugin.getTestedVariantData

 class KotlinJvmAndroidCompilation(
     target: KotlinAndroidTarget,
     name: String
 ) : AbstractKotlinCompilationToRunnableFiles<KotlinJvmOptions>(target, name) {

    override val kotlinOptions: KotlinJvmOptions = KotlinJvmOptionsImpl()

    lateinit var androidVariant: BaseVariant
        internal set

     override val compileKotlinTask: org.jetbrains.kotlin.gradle.tasks.KotlinCompile
         get() = super.compileKotlinTask as org.jetbrains.kotlin.gradle.tasks.KotlinCompile

     @Suppress("UNCHECKED_CAST")
     override val compileKotlinTaskProvider: TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>
         get() = super.compileKotlinTaskProvider as TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>


     @Suppress("UnstableApiUsage")
     internal val testedVariantArtifacts: Property<FileCollection> = target.project.objects.property(FileCollection::class.java)

     override val friendArtifacts: FileCollection get() = target.project.files(super.friendArtifacts, testedVariantArtifacts)

     override fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
         if ((other as? KotlinJvmAndroidCompilation)?.androidVariant != getTestedVariantData(androidVariant)) {
             super.addAssociateCompilationDependencies(other)
         } // otherwise, do nothing: the Android Gradle plugin adds these dependencies for us, we don't need to add them to the classpath
     }

     override val relatedConfigurationNames: List<String>
         get() = super.relatedConfigurationNames + listOf(
             "${androidVariant.name}ApiElements",
             "${androidVariant.name}RuntimeElements",
             androidVariant.compileConfiguration.name,
             androidVariant.runtimeConfiguration.name
         )

     val compileJavaTaskProvider: TaskProvider<out JavaCompile>?
         get() = androidVariant.getJavaTaskProvider()
 }