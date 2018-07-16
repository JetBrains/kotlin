package org.jetbrains.kotlin.gradle.plugin.experimental.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.internal.attributes.ImmutableAttributesFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.cpp.CppBinary.DEBUGGABLE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.gradle.nativeplatform.test.plugins.NativeTestingBasePlugin
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary.Companion.KONAN_TARGET_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.*
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.OutputKind.Companion.getDevelopmentKind
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetFactory
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetImpl
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
            attribute(KONAN_TARGET_ATTRIBUTE, target.name)
        }
        return DefaultUsageContext(variantName + usageContextSuffix, usage, attributes)
    }

    private fun AbstractKotlinNativeComponent.getAndLockTargets(): Set<KonanTarget> {
        konanTargets.lockNow()
        return konanTargets.get().also {
            require(it.isNotEmpty()) { "A Kotlin/Native target needs to be specified for the component." }
        }
    }

    private fun KotlinNativeMainComponent.getAndLockOutputKinds(): Set<OutputKind> {
        outputKinds.lockNow()
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
                for (buildType in KotlinNativeBuildType.DEFAULT_BUILD_TYPES) {
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
                        val variantIdentity = KotlinNativeVariantIdentity(
                                variantName,
                                component.baseName,
                                group, version, target,
                                buildType.debuggable,
                                buildType.optimized,
                                linkUsageContext,
                                runtimeUsageContext,
                                objects
                        )

                        val binary = component.addBinary(kind, variantIdentity)

                        if (kind.publishable) {
                            if (hostManager.isEnabled(target)) {
                                component.mainPublication.variants.add(binary)
                            } else {
                                // Known but not buildable.
                                // It allows us to publish different parts of a multitarget library from differnt hosts.
                                component.mainPublication.variants.add(variantIdentity)
                            }
                        }

                        if (kind == developmentKind &&
                            buildType == KotlinNativeBuildType.DEBUG &&
                            target == developmentTarget) {
                            component.developmentBinary.set(binary)
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

                val variantIdentity = KotlinNativeVariantIdentity(
                        variantName,
                        component.getBaseName(),
                        group, version, target,
                        buildType.debuggable,
                        buildType.optimized,
                        null,
                        null,
                        objects
                )

                val binary = component.addTestExecutable(variantIdentity)
                if (target == HostManager.host) {
                    component.testBinary.set(binary)
                }
            }
            component.binaries.realizeNow()
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
        val mainSourceSet = sourceSets.create("main").apply {
            kotlin.srcDir("src/main/kotlin")
            component = objectFactory
                    .newInstance(KotlinNativeMainComponent::class.java, name, this)
                    .apply {
                        // Override the default component base name.
                        baseName.set(project.name)
                        project.components.add(this)
                    }
        }

        sourceSets.create("test").apply {
            kotlin.srcDir("src/test/kotlin")
            component = objectFactory
                    .newInstance(KotlinNativeTestSuite::class.java, name, this, mainSourceSet.component)
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
        }
    }
}
