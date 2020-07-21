/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.plugins.JavaPluginConvention
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder.Companion.getSourceSetName
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder.Companion.kotlinPluginWrapper
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder.Companion.kotlinProjectExtensionClass
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder.Companion.kotlinSourceSetClass
import java.io.File
import java.io.Serializable
import java.lang.Exception

interface KotlinTaskProperties : Serializable {
    val incremental: Boolean?
    val packagePrefix: String?
    val pureKotlinSourceFolders: List<File>?
    val pluginVersion: String?
}

data class KotlinTaskPropertiesImpl(
    override val incremental: Boolean?,
    override val packagePrefix: String?,
    override val pureKotlinSourceFolders: List<File>?,
    override val pluginVersion: String?
) : KotlinTaskProperties {
    constructor(kotlinTaskProperties: KotlinTaskProperties) : this(
        kotlinTaskProperties.incremental,
        kotlinTaskProperties.packagePrefix,
        kotlinTaskProperties.pureKotlinSourceFolders?.map { it }?.toList(),
        kotlinTaskProperties.pluginVersion
    )
}

typealias KotlinTaskPropertiesBySourceSet = MutableMap<String, KotlinTaskProperties>

private fun Task.getPackagePrefix(): String? {
    try {
        val getJavaPackagePrefix = this.javaClass.getMethod("getJavaPackagePrefix")
        @Suppress("UNCHECKED_CAST")
        return (getJavaPackagePrefix.invoke(this) as? String)
    } catch (e: Exception) {
    }
    return null
}

private fun Task.getIsIncremental(): Boolean? {
    try {
        val abstractKotlinCompileClass = javaClass.classLoader.loadClass(AbstractKotlinGradleModelBuilder.ABSTRACT_KOTLIN_COMPILE_CLASS)
        val getIncremental = abstractKotlinCompileClass.getDeclaredMethod("getIncremental")
        @Suppress("UNCHECKED_CAST")
        return (getIncremental.invoke(this) as? Boolean)
    } catch (e: Exception) {
    }
    return null
}

private fun Task.getPureKotlinSourceRoots(sourceSet: String, disambiguationClassifier: String? = null): List<File>? {
    try {
        val kotlinExtensionClass = project.extensions.findByType(javaClass.classLoader.loadClass(kotlinProjectExtensionClass))
        val getKotlinMethod = javaClass.classLoader.loadClass(kotlinSourceSetClass).getMethod("getKotlin")
        val kotlinSourceSet = (kotlinExtensionClass?.javaClass?.getMethod("getSourceSets")?.invoke(kotlinExtensionClass)
                as? FactoryNamedDomainObjectContainer<Any>)?.asMap?.get(compilationFullName(sourceSet, disambiguationClassifier)) ?: return null
        val javaSourceSet =
            (project.convention.getPlugin(JavaPluginConvention::class.java) as JavaPluginConvention).sourceSets.asMap[sourceSet]
        val pureJava = javaSourceSet?.java?.srcDirs

        return (getKotlinMethod.invoke(kotlinSourceSet) as? SourceDirectorySet)?.srcDirs?.filter {
            !(pureJava?.contains(it) ?: false)
        }?.toList()
    } catch (e: Exception) {
    }
    return null
}

private fun Task.getKotlinPluginVersion(): String? {
    try {
        val pluginWrapperClass = javaClass.classLoader.loadClass(kotlinPluginWrapper)
        val getVersionMethod =
            pluginWrapperClass.getMethod("getKotlinPluginVersion", javaClass.classLoader.loadClass("org.gradle.api.Project"))
        return getVersionMethod.invoke(null, this.project) as String
    } catch (e: Exception) {
    }
    return null
}

fun KotlinTaskPropertiesBySourceSet.acknowledgeTask(compileTask: Task, classifier: String?) {
    this[compileTask.getSourceSetName()] =
        getKotlinTaskProperties(compileTask, classifier)
}

fun getKotlinTaskProperties(compileTask: Task, classifier: String?): KotlinTaskPropertiesImpl {
    return KotlinTaskPropertiesImpl(
        compileTask.getIsIncremental(),
        compileTask.getPackagePrefix(),
        compileTask.getPureKotlinSourceRoots(compileTask.getSourceSetName(), classifier),
        compileTask.getKotlinPluginVersion()
    )
}
