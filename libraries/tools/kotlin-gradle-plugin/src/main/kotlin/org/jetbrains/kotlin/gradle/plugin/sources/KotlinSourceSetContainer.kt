/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Project
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.DefaultSourceSetOutput
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProvider
import org.jetbrains.kotlin.gradle.plugin.addConvention
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import java.io.File

abstract class KotlinSourceSetContainer<T : KotlinSourceSet> internal constructor(
    itemClass: Class<T>,
    instantiator: Instantiator,
    protected val fileResolver: FileResolver,
    protected val project: Project
) : AbstractNamedDomainObjectContainer<T>(itemClass, instantiator), KotlinSourceSetProvider {

    // Needs setting when the plugin is applied. See `registerKotlinSourceSetsIfAbsent`.
    lateinit var kotlinProjectExtension: KotlinProjectExtension

    protected open fun defaultSourceLocation(sourceSetName: String): File =
        project.file("src/$sourceSetName")

    protected open fun setUpSourceSetDefaults(sourceSet: T) {
        with(sourceSet) {
            sourceSet.kotlin.srcDir(File(defaultSourceLocation(sourceSet.name), "kotlin"))
        }
    }

    protected abstract fun doCreateSourceSet(name: String): T

    final override fun doCreate(name: String): T {
        val result = doCreateSourceSet(name)
        setUpSourceSetDefaults(result)
        return result
    }

    final override fun provideSourceSet(displayName: String): T =
        maybeCreate(displayName)
}

// TODO maybe simplify to Project.container(<...>, factory)
class DefaultKotlinSourceSetContainer(
    project: Project,
    fileResolver: FileResolver,
    instantiator: Instantiator,
    private val taskResolver: TaskResolver
) : KotlinSourceSetContainer<DefaultKotlinSourceSet>(DefaultKotlinSourceSet::class.java, instantiator, fileResolver, project) {

    override fun setUpSourceSetDefaults(sourceSet: DefaultKotlinSourceSet) {
        super.setUpSourceSetDefaults(sourceSet)
        sourceSet.resources.srcDir(File(defaultSourceLocation(sourceSet.name), "resources"))
    }

    override fun doCreateSourceSet(name: String): DefaultKotlinSourceSet {
        val newSourceSetOutput = instantiator.newInstance(DefaultSourceSetOutput::class.java, name, fileResolver, taskResolver)
        return DefaultKotlinSourceSet(project, name, fileResolver)
    }
}