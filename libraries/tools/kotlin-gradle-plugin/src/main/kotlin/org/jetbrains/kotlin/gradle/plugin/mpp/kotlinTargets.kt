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
import org.gradle.api.attributes.Usage.JAVA_API
import org.gradle.api.attributes.Usage.JAVA_RUNTIME_JARS
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.ConfigureUtil
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class AbstractKotlinTarget(
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
        if (isGradleVersionAtLeast(4, 7)) {
            createKotlinTargetComponent()
        } else {
            KotlinVariant(this)
        }
    }

    private fun createKotlinTargetComponent(): KotlinTargetComponent {
        val kotlinExtension = project.kotlinExtension as? KotlinMultiplatformExtension
            ?: return KotlinVariantWithCoordinates(this)

        if (targetName == KotlinMultiplatformPlugin.METADATA_TARGET_NAME)
            return KotlinVariantWithCoordinates(this)

        val separateMetadataTarget = kotlinExtension.targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME)

        return if (kotlinExtension.isGradleMetadataAvailable) {
            KotlinVariantWithMetadataVariant(this, separateMetadataTarget)
        } else {
            // we should only add the Kotlin metadata dependency if we publish no Gradle metadata related to Kotlin MPP;
            // with metadata, such a dependency would get invalid, since a platform module should only depend on modules for that
            // same platform, not Kotlin metadata modules
            KotlinVariantWithMetadataDependency(this, separateMetadataTarget)
        }
    }

    override fun createUsageContexts(): Set<UsageContext> {
        val publishWithKotlinMetadata = (project.kotlinExtension as? KotlinMultiplatformExtension)?.isGradleMetadataAvailable
            ?: true // In non-MPP project, these usage contexts do not get published anyway

        // Here, `JAVA_API` and `JAVA_RUNTIME_JARS` are used intentionally as Gradle needs this for
        // ordering of the usage contexts (prioritizing the dependencies);
        // These Java usages should not be replaced with the custom Kotlin usages.
        return listOfNotNull(
            KotlinPlatformUsageContext(
                this, project.usageByName(JAVA_API),
                apiElementsConfigurationName, publishWithKotlinMetadata
            ),
            if (compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) is KotlinCompilationToRunnableFiles) {
                KotlinPlatformUsageContext(
                    this, project.usageByName(JAVA_RUNTIME_JARS),
                    runtimeElementsConfigurationName, publishWithKotlinMetadata
                )
            } else null
        ).toSet()
    }

    @Suppress("UNCHECKED_CAST")
    internal val publicationConfigureActions =
        WrapUtil.toDomainObjectSet(Action::class.java) as DomainObjectSet<Action<MavenPublication>>

    override fun mavenPublication(action: Action<MavenPublication>) {
        publicationConfigureActions.add(action)
    }

    override fun mavenPublication(action: Closure<Unit>) =
        mavenPublication(ConfigureUtil.configureUsing(action))
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
        project.container(
            KotlinWithJavaCompilation::class.java,
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

