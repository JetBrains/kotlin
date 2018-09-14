/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.ConfigureUtil
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class AbstractKotlinTarget (
    final override val project: Project
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

    override val publishable: Boolean
        get() = true

    override val component: KotlinTargetComponent by lazy {
        if (isGradleVersionAtLeast(4, 7))
            KotlinVariantWithCoordinates(this)
        else
            KotlinVariant(this)
    }

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

    @Suppress("UNCHECKED_CAST")
    internal val publicationConfigureActions =
        WrapUtil.toDomainObjectSet(Action::class.java) as DomainObjectSet<Action<MavenPublication>>

    override fun publication(action: Action<MavenPublication>) {
        publicationConfigureActions.add(action)
    }

    override fun publication(action: Closure<Unit>) =
        publication(ConfigureUtil.configureUsing(action))
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

    override val publishable: Boolean
        get() = konanTarget.enabledOnCurrentHost

    companion object {
        val konanTargetAttribute = Attribute.of(
            "org.jetbrains.kotlin.native.target",
            String::class.java
        )

        // TODO: Can we do it better?
        // User-visible constants
        val DEBUG = NativeBuildType.DEBUG
        val RELEASE = NativeBuildType.RELEASE

        val EXECUTABLE = NativeOutputKind.EXECUTABLE
        val FRAMEWORK = NativeOutputKind.FRAMEWORK
        val DYNAMIC = NativeOutputKind.DYNAMIC
        val STATIC = NativeOutputKind.STATIC
    }
}

