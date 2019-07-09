/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.experimental.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.internal.attributes.ImmutableAttributesFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.internal.reflect.Instantiator
import org.gradle.jvm.tasks.Jar
import org.gradle.language.cpp.CppBinary.DEBUGGABLE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.gradle.nativeplatform.test.plugins.NativeTestingBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary.Companion.KONAN_TARGET_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.*
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.OutputKind.Companion.getDevelopmentKind
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetFactory
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetImpl
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

class KotlinNativePlugin @Inject constructor(val attributesFactory: ImmutableAttributesFactory)
    : Plugin<ProjectInternal> {

    val hostManager = HostManager()

    private val Collection<*>.isDimensionVisible: Boolean
        get() = size > 1

    private fun createDimensionSuffix(dimensionName: String, multivalueProperty: Collection<*>): String =
            if (multivalueProperty.isDimensionVisible) {
                dimensionName.toLowerCase().capitalize()
            } else {
                ""
            }

    private fun createUsageContext(
            usageName: String,
            variantName: String,
            usageContextSuffix: String,
            buildType: KotlinNativeBuildType,
            target: KonanTarget,
            objectFactory: ObjectFactory
    ): DefaultUsageContext {
        val usage = objectFactory.named(Usage::class.java, usageName)
        val attributes = attributesFactory.mutable().apply {
            attribute(Usage.USAGE_ATTRIBUTE, usage)
            attribute(DEBUGGABLE_ATTRIBUTE, buildType.debuggable)
            attribute(OPTIMIZED_ATTRIBUTE, buildType.optimized)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
            attribute(KONAN_TARGET_ATTRIBUTE, target.name)
        }
        return DefaultUsageContext(variantName + usageContextSuffix, attributes)
    }

    private fun AbstractKotlinNativeComponent.getAndLockTargets(): Set<KonanTarget> {
        if (isGradleVersionAtLeast(5, 0)) {
            konanTargets.finalizeValue()
        }
        return konanTargets.get().also {
            require(it.isNotEmpty()) { "A Kotlin/Native target needs to be specified for the component." }
        }
    }

    private fun KotlinNativeMainComponent.getAndLockOutputKinds(): Set<OutputKind> {
        if (isGradleVersionAtLeast(5, 0)) {
            konanTargets.finalizeValue()
        }
        return outputKinds.get().also {
            require(it.isNotEmpty()) { "An output kind needs to be specified for the component." }
        }
    }

    private fun Collection<KonanTarget>.getDevelopmentTarget(): KonanTarget =
            if (contains(HostManager.host)) HostManager.host else first()

    private fun Project.addBinariesForMainComponents(group: Provider<String>, version: Provider<String>) {
        for (component in components.withType(KotlinNativeMainComponent::class.java)) {
            val targets = component.getAndLockTargets()
            val outputKinds = component.getAndLockOutputKinds()
            val developmentKind = outputKinds.getDevelopmentKind()
            val developmentTarget = targets.getDevelopmentTarget()
            val objectFactory = objects
            val hostManager = HostManager()

            for (kind in outputKinds) {
                val buildTypes = if (kind == OutputKind.KLIBRARY)
                    listOf(KotlinNativeBuildType.DEBUG)
                else
                    KotlinNativeBuildType.DEFAULT_BUILD_TYPES
                for (buildType in buildTypes) {
                    for (target in targets.filter { kind.availableFor(it) }) {

                        val buildTypeSuffix = buildType.name
                        val outputKindSuffix = createDimensionSuffix(kind.name, outputKinds)
                        val targetSuffix = createDimensionSuffix(target.name, targets)
                        val variantName = "${buildTypeSuffix}${outputKindSuffix}${targetSuffix}"

                        val linkUsageContext: DefaultUsageContext? = kind.linkUsageName?.let {
                            createUsageContext(it, variantName, "Link", buildType, target, objectFactory)
                        }

                        val runtimeUsageContext = kind.runtimeUsageName?.let {
                            createUsageContext(it, variantName, "Runtime", buildType, target, objectFactory)
                        }

                        // TODO: Do we need something like klibUsageContext?
                        val variant = KotlinNativeVariant(
                                variantName,
                                component.baseName,
                                group, version, target,
                                buildType,
                                linkUsageContext,
                                runtimeUsageContext,
                                project
                        )

                        if (hostManager.isEnabled(target)) {
                            val binary = component.addBinary(kind, variant)

                            if (kind == developmentKind &&
                                buildType == KotlinNativeBuildType.DEBUG &&
                                target == developmentTarget) {
                                component.developmentBinary.set(binary)
                            }

                            if (kind.publishable) {
                                component.mainPublication.variants.add(binary)
                            }

                        } else {
                            if (kind.publishable) {
                                // Known but not buildable.
                                // It allows us to publish different parts of a multitarget library from differnt hosts.
                                component.mainPublication.variants.add(variant.identity)
                            }
                        }
                    }
                }
            }
            component.binaries.realizeNow()
        }
    }

    private fun Project.addBinariesForTestComponents(group: Provider<String>, version: Provider<String>) {
        for (component in components.withType(KotlinNativeTestSuite::class.java)) {
            val targets = component.getAndLockTargets()
            val buildType = KotlinNativeBuildType.DEBUG

            for (target in targets) {
                val buildTypeSuffix = buildType.name
                val targetSuffix = createDimensionSuffix(target.name, targets)
                val variantName = "${buildTypeSuffix}${targetSuffix}"

                val variant = KotlinNativeVariant(
                        variantName,
                        component.getBaseName(),
                        group, version, target,
                        buildType,
                        null,
                        null,
                        project
                )

                if (hostManager.isEnabled(target)) {
                    val binary = component.addTestExecutable(variant)
                    if (target == HostManager.host) {
                        component.testBinary.set(binary)
                    }
                }
            }
            component.binaries.realizeNow()
        }
    }

    private fun KotlinNativeSourceSetImpl.createJarTask(taskName: String, configure: (Jar) -> Unit): Jar {
        val task = project.tasks.findByName(taskName)
        return if (task != null) {
            task as Jar
        } else {
            project.tasks.create(taskName, Jar::class.java, configure)
        }
    }

    private fun KotlinNativeSourceSetImpl.createSourcesJarTask(target: KonanTarget): Jar =
            createJarTask("sourcesJar${name.capitalize()}${target.name.capitalize()}") {
                it.destinationDir = project.buildDir.resolve("libs")
                it.appendix = "$name-${target.name}"
                it.classifier = "sources"
                it.from(getAllSources(target))
            }

    private fun KotlinNativeSourceSetImpl.createEmptyJarTask(namePrefix: String, classifier: String): Jar =
            createJarTask("$namePrefix${name.capitalize()}") {
                it.destinationDir = project.buildDir.resolve("libs")
                it.appendix = name
                it.classifier = classifier
            }


    private fun Project.setUpMavenPublish() =  pluginManager.withPlugin("maven-publish") { _ ->
        val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
        loop@for (publication in publishingExtension.publications) {

            if (publication !is MavenPublication) continue
            val publicationComponent = components.find { it.name == publication.name } ?: continue

            val sourcesJar: Jar
            val mainComponent: AbstractKotlinNativeComponent

            when (publicationComponent) {
                is KotlinNativeMainComponent -> {
                    mainComponent = publicationComponent
                    sourcesJar = mainComponent.sources.createEmptyJarTask("sourcesJar", "sources")
                }
                is AbstractKotlinNativeBinary -> {
                    mainComponent = publicationComponent.component
                    sourcesJar = mainComponent.sources.createSourcesJarTask(publicationComponent.konanTarget)
                }
                else -> {
                    logger.info("Unknown component type: $publicationComponent, ${publicationComponent::class.java}")
                    continue@loop
                }
            }
            val javadocJar = mainComponent.sources.createEmptyJarTask("javadocJar", "javadoc")

            with(mainComponent) {
                poms.forEach { publication.pom(it) }
                if (publishSources) { publication.artifact(sourcesJar) }
                if (publishJavadoc) { publication.artifact(javadocJar) }
            }
        }
    }

    override fun apply(project: ProjectInternal): Unit = with(project) {
        pluginManager.apply(KotlinNativeBasePlugin::class.java)
        pluginManager.apply(NativeTestingBasePlugin::class.java)

        val instantiator = services.get(Instantiator::class.java)
        val objectFactory = objects

        @Suppress("UNCHECKED_CAST")
        val sourceSets = project.extensions.create(
                KotlinNativeBasePlugin.SOURCE_SETS_EXTENSION,
                FactoryNamedDomainObjectContainer::class.java,
                KotlinNativeSourceSetImpl::class.java,
                instantiator,
                KotlinNativeSourceSetFactory(this)
        ) as FactoryNamedDomainObjectContainer<KotlinNativeSourceSetImpl>

        // TODO: We should be able to create a component automatically once
        // a source set is created by user. So we need a user DSL or some another way
        // to determine which component class should be instantiated (main or test).
        val mainSourceSet = sourceSets.create(MAIN_SOURCE_SET_NAME).apply {
            kotlin.srcDir("src/$MAIN_SOURCE_SET_NAME/kotlin")
            component = objectFactory
                    .newInstance(KotlinNativeMainComponent::class.java, name, this, project)
                    .apply {
                        // Override the default component base name.
                        baseName.set(project.name)
                        project.components.add(this)
                    }
        }

        sourceSets.create(TEST_SOURCE_SET_NAME).apply {
            kotlin.srcDir("src/$TEST_SOURCE_SET_NAME/kotlin")
            component = objectFactory
                    .newInstance(KotlinNativeTestSuite::class.java, name, this, mainSourceSet.component, project)
                    .apply {
                        project.components.add(this)
                    }
        }
        // Create binaries for host
        afterEvaluate {
            val group = project.provider { project.group.toString() }
            val version = project.provider { project.version.toString() }

            addBinariesForMainComponents(group, version)
            addBinariesForTestComponents(group, version)
            setUpMavenPublish()
        }
    }

    companion object {
        const val MAIN_SOURCE_SET_NAME = "main"
        const val TEST_SOURCE_SET_NAME = "test"
    }
}
