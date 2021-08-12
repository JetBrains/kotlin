/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyConfigurationForPublishing
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.listProperty
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
import org.jetbrains.kotlin.gradle.utils.lowerCaseDashSeparatedName
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class KotlinPm20GradlePlugin @Inject constructor(
    @Inject private val softwareComponentFactory: SoftwareComponentFactory
) : Plugin<Project> {
    override fun apply(project: Project) {
        checkGradleCompatibility("the Kotlin Multiplatform plugin", GradleVersion.version("6.1"))

        // Gradle sets up the attribute schema for consuming JVM dependencies in the JavaBasePlugin
        project.plugins.apply(JavaBasePlugin::class.java)

        createDefaultModules(project)
        customizeKotlinDependencies(project)
        registerDefaultVariantFactories(project)
        setupFragmentsMetadata(project)
        setupPublication(project)
    }

    private fun registerDefaultVariantFactories(project: Project) {
        project.pm20Extension.modules.configureEach { module ->
            module.fragments.registerFactory(
                KotlinJvmVariant::class.java,
                KotlinJvmVariantFactory(module)
            )

            fun <T: KotlinNativeVariantInternal> registerNativeVariantFactory(variantClass: KClass<T>) {
                module.fragments.registerFactory(
                    variantClass.java,
                    KotlinNativeVariantFactory(module, variantClass)
                )
            }
            listOf(
                // FIXME codegen, add missing native targets
                KotlinLinuxX64Variant::class,
                KotlinMacosX64Variant::class,
                KotlinMacosArm64Variant::class,
                KotlinIosX64Variant::class,
                KotlinIosArm64Variant::class
            ).forEach { variantClass ->
                registerNativeVariantFactory(variantClass)
            }
        }
    }

    private fun createDefaultModules(project: Project) {
        project.pm20Extension.apply {
            modules.create(KotlinGradleModule.MAIN_MODULE_NAME)
            modules.create(KotlinGradleModule.TEST_MODULE_NAME)
            main { makePublic(Standalone(null)) }
        }
    }

    private fun setupFragmentsMetadata(project: Project) {
        project.pm20Extension.modules.all { module ->
            configureMetadataResolutionAndBuild(module)
            configureMetadataExposure(module)
        }
    }

    private fun setupPublication(project: Project) {
        project.pm20Extension.modules.all { module ->
            setupPublicationForModule(module)
        }
    }

    private fun setupPublicationForModule(module: KotlinGradleModule) {
        val project = module.project

        module.ifMadePublic {
            val publicationHolder: SingleMavenPublishedModuleHolder? = module.publicationHolder()

            val metadataElements = project.configurations.getByName(metadataElementsConfigurationName(module))
            val sourceElements = project.configurations.getByName(sourceElementsConfigurationName(module))

            val publishedConfigurationNameSuffix = "-published"
            val metadataElementsForPublishing = copyConfigurationForPublishing(
                project,
                metadataElements.name + publishedConfigurationNameSuffix,
                metadataElements,
                overrideDependencies = {
                    addAllLater(project.listProperty {
                        replaceProjectDependenciesWithPublishedMavenDependencies(project, metadataElements.allDependencies)
                    })
                },
                overrideCapabilities = { setGradlePublishedModuleCapability(this, module) }
            )

            val sourceElementsForPublishing = copyConfigurationForPublishing(
                project,
                sourceElements.name + publishedConfigurationNameSuffix,
                sourceElements,
                overrideCapabilities = { setGradlePublishedModuleCapability(this, module) }
            )

            fun addVariantsToSoftwareComponent(component: AdhocComponentWithVariants) {
                component.addVariantsFromConfiguration(metadataElementsForPublishing) { }
                component.addVariantsFromConfiguration(sourceElementsForPublishing) { }
            }

            val rootSoftwareComponent = when (module.publicationMode) {
                is Standalone -> {
                    val component = softwareComponentFactory.adhoc(rootPublicationComponentName(module))
                    project.components.add(component)
                    component
                }
                is Embedded -> {
                    project.components.withType(AdhocComponentWithVariants::class.java)
                        .getByName(rootPublicationComponentName(project.pm20Extension.main))
                }
                Private -> error("unexpected private module; expected publicationMode: Standalone or Embedded")
            }

            addVariantsToSoftwareComponent(rootSoftwareComponent)

            project.pluginManager.withPlugin("maven-publish") {
                val publishing = project.extensions.getByType(PublishingExtension::class.java)
                when (val publicationMode = module.publicationMode) {
                    is Standalone -> {
                        project.pluginManager.withPlugin("maven-publish") {
                            publishing.publications.create(rootSoftwareComponent.name, MavenPublication::class.java) { publication ->
                                if (!module.isMain) {
                                    (publication as DefaultMavenPublication).isAlias = true
                                }
                                publication.artifactId = lowerCaseDashSeparatedName(project.name, publicationMode.defaultArtifactIdSuffix)
                                publication.from(rootSoftwareComponent)
                                publicationHolder?.assignMavenPublication(publication)
                            }
                        }
                    }
                    Embedded -> {
                        val mainPublication = publishing.publications.withType(MavenPublication::class.java).getByName(rootSoftwareComponent.name)
                        publicationHolder?.assignMavenPublication(mainPublication)
                    }
                }
            }
        }
    }
}

fun rootPublicationComponentName(module: KotlinGradleModule) =
    module.disambiguateName("root")

open class KotlinPm20ProjectExtension(project: Project) : KotlinTopLevelExtension(project), HasKotlinDependencies {
    val modules: NamedDomainObjectContainer<KotlinGradleModule> =
        project.objects.domainObjectContainer(
            KotlinGradleModule::class.java,
            KotlinGradleModuleFactory(project)
        )

    @Suppress("unused") // DSL function
    fun mainAndTest(configure: KotlinGradleModule.() -> Unit) {
        main(configure)
        test(configure)
    }

    val main: KotlinGradleModule
        get() = modules.getByName(KotlinGradleModule.MAIN_MODULE_NAME)

    val test: KotlinGradleModule
        get() = modules.getByName(KotlinGradleModule.TEST_MODULE_NAME)

    fun main(configure: KotlinGradleModule.() -> Unit = { }) = main.apply(configure)
    fun test(configure: KotlinGradleModule.() -> Unit = { }) = test.apply(configure)

    internal val metadataCompilationRegistryByModuleId: MutableMap<KotlinModuleIdentifier, MetadataCompilationRegistry> =
        mutableMapOf()

    internal var rootPublication: MavenPublication? = null

    @PublishedApi
    @JvmName("isAllowCommonizer")
    internal fun isAllowCommonizerForIde(@Suppress("UNUSED_PARAMETER") project: Project): Boolean = false

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) = main.dependencies(configure)
    override fun dependencies(configureClosure: Closure<Any?>) = main.dependencies(configureClosure)

    override val apiConfigurationName: String get() = main.apiConfigurationName
    override val implementationConfigurationName: String get() = main.implementationConfigurationName
    override val compileOnlyConfigurationName: String get() = main.compileOnlyConfigurationName
    override val runtimeOnlyConfigurationName: String get() = main.runtimeOnlyConfigurationName
}

val KotlinGradleModule.jvm: KotlinJvmVariant
    get() = fragments.maybeCreate("jvm", KotlinJvmVariant::class.java)

fun KotlinGradleModule.jvm(configure: KotlinJvmVariant.() -> Unit): KotlinJvmVariant = jvm.apply(configure)

fun KotlinPm20ProjectExtension.jvm(configure: KotlinFragmentSlice<KotlinJvmVariant>.() -> Unit) {
    val getOrCreateVariant: KotlinGradleModule.() -> KotlinJvmVariant = { jvm }
    mainAndTest { getOrCreateVariant(this) }
    val slice = KotlinFragmentSlice(this, getOrCreateVariant)
    configure(slice)
}

open class KotlinFragmentSlice<T : KotlinGradleFragment>(
    val pm20ProjectExtension: KotlinPm20ProjectExtension,
    val getOrCreateFragment: (KotlinGradleModule) -> T
) {
    fun inMain(configure: T.() -> Unit) {
        pm20ProjectExtension.modules.getByName(KotlinGradleModule.MAIN_MODULE_NAME).apply {
            getOrCreateFragment(this).configure()
        }
    }

    fun inTest(configure: T.() -> Unit) {
        pm20ProjectExtension.modules.getByName(KotlinGradleModule.TEST_MODULE_NAME).apply {
            getOrCreateFragment(this).configure()
        }
    }

    fun inMainAndTest(configure: T.() -> Unit) {
        pm20ProjectExtension.mainAndTest { getOrCreateFragment(this).configure() }
    }

    fun inAllModules(configure: T.() -> Unit) {
        pm20ProjectExtension.modules.all {
            getOrCreateFragment(it).configure()
        }
    }

    fun inModule(moduleName: String, configure: T.() -> Unit) {
        pm20ProjectExtension.modules.getByName(moduleName).apply { getOrCreateFragment(this).configure() }
    }

    fun inModule(module: NamedDomainObjectProvider<KotlinGradleModule>, configure: T.() -> Unit) {
        module.get().apply { inModule(this, configure) }
    }

    fun inModule(module: KotlinGradleModule, configure: T.() -> Unit) {
        getOrCreateFragment(module).configure()
    }
}