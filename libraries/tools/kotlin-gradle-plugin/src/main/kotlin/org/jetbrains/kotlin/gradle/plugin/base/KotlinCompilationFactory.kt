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

class KotlinCommonCompilationFactory(
    val project: Project,
    val target: KotlinOnlyTarget<KotlinCommonCompilation>
) : KotlinCompilationFactory<KotlinCommonCompilation> {
    override val itemClass: Class<KotlinCommonCompilation>
        get() = KotlinCommonCompilation::class.java

    override fun create(p0: String?): KotlinCommonCompilation {
        TODO("not implemented")
    }
}

class KotlinJvmCompilationFactory(
    val project: Project,
    val target: KotlinOnlyTarget<KotlinJvmCompilation>
) : KotlinCompilationFactory<KotlinJvmCompilation> {
    override val itemClass: Class<KotlinJvmCompilation>
        get() = KotlinJvmCompilation::class.java

    override fun create(name: String): KotlinJvmCompilation = KotlinJvmCompilation(target, name, )
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

class KotlinJsCompilationFactory(val project: Project) : KotlinCompilationFactory<KotlinJsCompilation> {
    override val itemClass: Class<KotlinJsCompilation>
        get() = KotlinJsCompilation::class.java

    override fun create(p0: String?): KotlinJsCompilation {
        TODO("not implemented")
    }
}