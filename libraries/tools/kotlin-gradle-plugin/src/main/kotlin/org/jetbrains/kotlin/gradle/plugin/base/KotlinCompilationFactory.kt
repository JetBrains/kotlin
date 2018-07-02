/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.base

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.*

interface KotlinCompilationFactory<T: KotlinCompilation> : NamedDomainObjectFactory<T> {
    val itemClass: Class<T>
}

private fun Project.createSourceSetOutput(name: String) =
    KotlinSourceSetOutput(this, buildDir.resolve("processedResources/$name"))

class KotlinCommonCompilationFactory(
    val project: Project,
    val target: KotlinOnlyTarget<KotlinCommonCompilation>
) : KotlinCompilationFactory<KotlinCommonCompilation> {
    override val itemClass: Class<KotlinCommonCompilation>
        get() = KotlinCommonCompilation::class.java

    override fun create(name: String): KotlinCommonCompilation =
        KotlinCommonCompilation(target, name, project.createSourceSetOutput(name))
}

class KotlinJvmCompilationFactory(
    val project: Project,
    val target: KotlinOnlyTarget<KotlinJvmCompilation>
) : KotlinCompilationFactory<KotlinJvmCompilation> {
    override val itemClass: Class<KotlinJvmCompilation>
        get() = KotlinJvmCompilation::class.java

    override fun create(name: String): KotlinJvmCompilation = KotlinJvmCompilation(
        target,
        name,
        KotlinSourceSetOutput(project, project.buildDir.resolve("resources/$name"))
    )
}

class KotlinJvmWithJavaCompilationFactory(
    val project: Project,
    val target: KotlinWithJavaTarget
) : KotlinCompilationFactory<KotlinJvmWithJavaCompilation> {
    private val javaSourceSets: SourceSetContainer
        get() = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets

    override val itemClass: Class<KotlinJvmWithJavaCompilation>
        get() = KotlinJvmWithJavaCompilation::class.java

    override fun create(name: String): KotlinJvmWithJavaCompilation {
        val javaSourceSet = javaSourceSets.maybeCreate(name)
        val result = KotlinJvmWithJavaCompilation(target, name, javaSourceSet)
        return result
    }
}

class KotlinJvmAndroidCompilationFactory(
    val project: Project,
    val target: KotlinAndroidTarget
) : KotlinCompilationFactory<KotlinJvmAndroidCompilation> {
    private val javaSourceSets: SourceSetContainer
        get() = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets

    override val itemClass: Class<KotlinJvmAndroidCompilation>
        get() = KotlinJvmAndroidCompilation::class.java

    override fun create(name: String): KotlinJvmAndroidCompilation {
        val output = project.createSourceSetOutput(name)
        val result = KotlinJvmAndroidCompilation(target, name, output)
        return result
    }
}

class Kotlin

class KotlinJsCompilationFactory(
    val project: Project,
    val target: KotlinOnlyTarget<KotlinJsCompilation>
) : KotlinCompilationFactory<KotlinJsCompilation> {
    override val itemClass: Class<KotlinJsCompilation>
        get() = KotlinJsCompilation::class.java

    override fun create(name: String): KotlinJsCompilation =
            KotlinJsCompilation(target, name, project.createSourceSetOutput(name))
}