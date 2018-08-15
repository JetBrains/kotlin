/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.plugins.JavaPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget

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

    override fun toString(): String = "target $name ($platformType)"

    override fun createUsageContexts(): Set<UsageContext> =
        setOf(
            KotlinPlatformUsageContext(
                project, this, KotlinSoftwareComponent.kotlinApiUsage(project), apiElementsConfigurationName
            )
        ) + if (compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) is KotlinCompilationToRunnableFiles)
            setOf(
                KotlinPlatformUsageContext(
                    project,
                    this,
                    KotlinSoftwareComponent.kotlinRuntimeUsage(project),
                    runtimeElementsConfigurationName
                )
            ) else emptyList()
}

internal fun KotlinTarget.disambiguateName(simpleName: String) =
    lowerCamelCaseName(targetName, simpleName)

open class KotlinAndroidTarget(
    override val targetName: String,
    project: Project
) : AbstractKotlinTarget(project) {

    override var disambiguationClassifier: String? = null
        internal set

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.androidJvm

    private val compilationFactory = KotlinJvmAndroidCompilationFactory(project, this)

    override val compilations: NamedDomainObjectContainer<out KotlinJvmAndroidCompilation> =
        project.container(compilationFactory.itemClass, compilationFactory)

    override fun createUsageContexts(): Set<UsageContext> {
        //TODO setup Android libraries publishing. This will likely require new API in the Android Gradle plugin
        return emptySet()
    }
}

open class KotlinWithJavaTarget(
    project: Project,
    override val platformType: KotlinPlatformType,
    override val targetName: String
) : AbstractKotlinTarget(project) {
    override var disambiguationClassifier: String? = null
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
        project.container(KotlinWithJavaCompilation::class.java,
                          KotlinWithJavaCompilationFactory(project, this)
        )
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
}

class KotlinNativeTarget(
    project: Project,
    val konanTarget: KonanTarget
) : KotlinOnlyTarget<KotlinNativeCompilation>(project, KotlinPlatformType.native) {

    init {
        attributes.attribute(konanTargetAttribute, konanTarget.name)
    }

    // TODO: Should binary files be output of a target or a compilation?
    override val artifactsTaskName: String
        get() = disambiguateName("link")

    companion object {
        val konanTargetAttribute = Attribute.of(
            "org.jetbrains.kotlin.native.target",
            String::class.java
        )
    }
}

