/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.plugins.JavaPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.base.KotlinJvmAndroidCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.base.KotlinWithJavaCompilationFactory
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

abstract class AbstractKotlinTarget (
    override val project: Project
) : KotlinTarget {
    private val attributeContainer = HierarchyAttributeContainer(parent = null)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val defaultConfigurationName: String
        get() = disambiguateName("default")

    override val apiElementsConfigurationName: String
        get() = disambiguateName("apiElements")

    override val runtimeElementsConfigurationName: String
        get() = disambiguateName("runtimeElements")

    override val artifactsTaskName: String
        get() = disambiguateName("jar")
}

internal fun KotlinTarget.disambiguateName(simpleName: String) =
    lowerCamelCaseName(targetName, simpleName)

open class KotlinAndroidTarget(project: Project) : AbstractKotlinTarget(project) {
    override val targetName: String
        get() = "android"

    override val disambiguationClassifier: String? get() = null

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm

    private val compilationFactory = KotlinJvmAndroidCompilationFactory(project, this)

    override val compilations: NamedDomainObjectContainer<out KotlinCompilation> =
        project.container(compilationFactory.itemClass, compilationFactory)
}

open class KotlinWithJavaTarget(
    project: Project,
    override val platformType: KotlinPlatformType
) : AbstractKotlinTarget(project) {
    override var disambiguationClassifier: String? = null
        internal set

    override var targetName: String = "kotlin"
        internal set

    override val defaultConfigurationName: String
        get() = Dependency.DEFAULT_CONFIGURATION

    override val apiElementsConfigurationName: String
        get() = JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME

    override val runtimeElementsConfigurationName: String
        get() = JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME

    override val artifactsTaskName: String
        get() = JavaPlugin.JAR_TASK_NAME

    override val compilations: NamedDomainObjectContainer<KotlinWithJavaCompilation> =
        project.container(KotlinWithJavaCompilation::class.java, KotlinWithJavaCompilationFactory(project, this))
}

open class KotlinOnlyTarget<T : KotlinCompilation>(
    project: Project,
    override val platformType: KotlinPlatformType
) : AbstractKotlinTarget(project) {

    override lateinit var compilations: NamedDomainObjectContainer<T>
        internal set

    override lateinit var targetName: String
        internal set

    override var disambiguationClassifier: String? = null
        internal set

    var userDefinedPlatformId: String? = null
}
