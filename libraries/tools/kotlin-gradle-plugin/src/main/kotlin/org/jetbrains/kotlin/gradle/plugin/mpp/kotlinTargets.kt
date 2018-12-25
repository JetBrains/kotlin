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
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.ConfigureUtil
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
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

    override val components: Set<KotlinTargetComponent> by lazy {
        val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        val usageContexts = createUsageContexts(mainCompilation, targetName, targetName.toLowerCase())
        setOf(
            if (isGradleVersionAtLeast(4, 7)) {
                createKotlinVariant(mainCompilation.target.name, mainCompilation, usageContexts)
            } else {
                KotlinVariant(mainCompilation, usageContexts)
            }
        ).also { project.components.addAll(it) }
    }

    protected fun createKotlinVariant(
        componentName: String,
        compilation: KotlinCompilation<*>,
        usageContexts: Set<DefaultKotlinUsageContext>
    ): KotlinVariant {

        val kotlinExtension = project.kotlinExtension as? KotlinMultiplatformExtension
            ?: return KotlinVariantWithCoordinates(compilation, usageContexts)

        if (targetName == KotlinMultiplatformPlugin.METADATA_TARGET_NAME)
            return KotlinVariantWithCoordinates(compilation, usageContexts)

        val separateMetadataTarget = kotlinExtension.targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME)

        val result = if (kotlinExtension.isGradleMetadataAvailable) {
            KotlinVariantWithMetadataVariant(compilation, usageContexts, separateMetadataTarget)
        } else {
            // we should only add the Kotlin metadata dependency if we publish no Gradle metadata related to Kotlin MPP;
            // with metadata, such a dependency would get invalid, since a platform module should only depend on modules for that
            // same platform, not Kotlin metadata modules
            KotlinVariantWithMetadataDependency(compilation, usageContexts, separateMetadataTarget)
        }

        result.componentName = componentName
        return result
    }

    private fun createUsageContexts(
        producingCompilation: KotlinCompilation<*>,
        componentName: String,
        artifactNameAppendix: String
    ): Set<DefaultKotlinUsageContext> {
        val publishWithKotlinMetadata = (project.kotlinExtension as? KotlinMultiplatformExtension)?.isGradleMetadataAvailable
            ?: true // In non-MPP project, these usage contexts do not get published anyway

        // Here, `JAVA_API` and `JAVA_RUNTIME_JARS` are used intentionally as Gradle needs this for
        // ordering of the usage contexts (prioritizing the dependencies) when merging them into the POM;
        // These Java usages should not be replaced with the custom Kotlin usages.
        return listOfNotNull(
            JAVA_API to apiElementsConfigurationName,
            (JAVA_RUNTIME_JARS to runtimeElementsConfigurationName).takeIf { producingCompilation is KotlinCompilationToRunnableFiles }
        ).mapTo(mutableSetOf()) { (usageName, dependenciesConfigurationName) ->
            DefaultKotlinUsageContext(
                producingCompilation,
                project.usageByName(usageName),
                dependenciesConfigurationName,
                publishWithKotlinMetadata
                )
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal val publicationConfigureActions =
        WrapUtil.toDomainObjectSet(Action::class.java) as DomainObjectSet<Action<MavenPublication>>

    override fun mavenPublication(action: Action<MavenPublication>) {
        publicationConfigureActions.add(action)
    }

    override fun mavenPublication(action: Closure<Unit>) =
        mavenPublication(ConfigureUtil.configureUsing(action))

    override var preset: KotlinTargetPreset<out KotlinTarget>? = null
        internal set
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

    override val components: Set<KotlinTargetComponent>
        get() = emptySet()
}

open class KotlinWithJavaTarget<KotlinOptionsType : KotlinCommonOptions>(
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

    override val compilations: NamedDomainObjectContainer<KotlinWithJavaCompilation<KotlinOptionsType>> =
        @Suppress("UNCHECKED_CAST")
        project.container(
            KotlinWithJavaCompilation::class.java as Class<KotlinWithJavaCompilation<KotlinOptionsType>>,
            KotlinWithJavaCompilationFactory(project, this)
        )
}

open class KotlinOnlyTarget<T : KotlinCompilation<*>>(
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

    val binaries = if(isGradleVersionAtLeast(4, 2)) {
        // Use newInstance to allow accessing binaries by their names in Groovy using the extension mechanism.
        project.objects.newInstance(KotlinNativeBinaryContainer::class.java, this, WrapUtil.toDomainObjectSet(NativeBinary::class.java))
    } else {
        KotlinNativeBinaryContainer(this, WrapUtil.toDomainObjectSet(NativeBinary::class.java))
    }

    fun binaries(configure: KotlinNativeBinaryContainer.() -> Unit) {
        binaries.configure()
    }

    fun binaries(configure: Closure<*>) {
        ConfigureUtil.configure(configure, binaries)
    }

    override val artifactsTaskName: String
        get() = disambiguateName("binaries")

    override val publishable: Boolean
        get() = konanTarget.enabledOnCurrentHost

    // User-visible constants
    val DEBUG = NativeBuildType.DEBUG
    val RELEASE = NativeBuildType.RELEASE

    val EXECUTABLE = NativeOutputKind.EXECUTABLE
    val FRAMEWORK = NativeOutputKind.FRAMEWORK
    val DYNAMIC = NativeOutputKind.DYNAMIC
    val STATIC = NativeOutputKind.STATIC

    companion object {
        val konanTargetAttribute = Attribute.of(
            "org.jetbrains.kotlin.native.target",
            String::class.java
        )
    }
}

