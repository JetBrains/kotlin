/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinProjectModelBuilder
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier
import javax.inject.Inject

abstract class KotlinPm20GradlePlugin @Inject constructor(
    @Inject private val softwareComponentFactory: SoftwareComponentFactory,
    @Inject private val toolingModelBuilderRegistry: ToolingModelBuilderRegistry
) : Plugin<Project> {
    override fun apply(project: Project) {
        checkGradleCompatibility("the Kotlin Multiplatform plugin", GradleVersion.version("6.1"))

        // Gradle sets up the attribute schema for consuming JVM dependencies in the JavaBasePlugin
        project.plugins.apply(JavaBasePlugin::class.java)

        createDefaultModules(project)
        customizeKotlinDependencies(project)
        registerDefaultVariantFactories(project)
        setupFragmentsMetadataForKpmModules(project)
        setupKpmModulesPublication(project)
        setupToolingModelBuilder()
    }

    private fun createDefaultModules(project: Project) {
        project.pm20Extension.apply {
            modules.create(KotlinGradleModule.MAIN_MODULE_NAME)
            modules.create(KotlinGradleModule.TEST_MODULE_NAME)
            main { makePublic() }
        }
    }

    private fun setupPublicationForModule(module: KotlinGradleModule) {
        val project = module.project

        val metadataElements = project.configurations.getByName(metadataElementsConfigurationName(module))
        val sourceElements = project.configurations.getByName(sourceElementsConfigurationName(module))

        val componentName = rootPublicationComponentName(module)
        val rootSoftwareComponent = softwareComponentFactory.adhoc(componentName).also {
            project.components.add(it)
            it.addVariantsFromConfiguration(metadataElements) { }
            it.addVariantsFromConfiguration(sourceElements) { }
        }

        module.ifMadePublic {
            val metadataDependencyConfiguration = resolvableMetadataConfiguration(module)
            project.pluginManager.withPlugin("maven-publish") {
                project.extensions.getByType(PublishingExtension::class.java).publications.create(
                    componentName,
                    MavenPublication::class.java
                ) { publication ->
                    publication.from(rootSoftwareComponent)
                    publication.versionMapping { versionMapping ->
                        versionMapping.allVariants {
                            it.fromResolutionOf(metadataDependencyConfiguration)
                        }
                    }
                }
            }
        }
    }

    private fun setupToolingModelBuilder() {
        toolingModelBuilderRegistry.register(IdeaKotlinProjectModelBuilder())
    }
}

fun rootPublicationComponentName(module: KotlinGradleModule) =
    module.disambiguateName("root")

open class KotlinPm20ProjectExtension(project: Project) : KotlinTopLevelExtension(project) {

    internal val kpmModelContainer = DefaultKpmGradleProjectModelContainer.create(project)

    val modules: NamedDomainObjectContainer<KotlinGradleModule>
        get() = project.kpmModules

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

    @PublishedApi
    @JvmName("isAllowCommonizer")
    internal fun isAllowCommonizerForIde(@Suppress("UNUSED_PARAMETER") project: Project): Boolean = false
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
