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
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinProjectModelBuilder
import org.jetbrains.kotlin.gradle.kpm.idea.default
import org.jetbrains.kotlin.gradle.kpm.idea.locateOrRegisterBuildIdeaKotlinProjectModelTask
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
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
        setupToolingModelBuilder(project)
    }

    private fun createDefaultModules(project: Project) {
        project.pm20Extension.apply {
            modules.create(KpmGradleModule.MAIN_MODULE_NAME)
            modules.create(KpmGradleModule.TEST_MODULE_NAME)
            main { makePublic() }
        }
    }

    private fun setupPublicationForModule(module: KpmGradleModule) {
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

    private fun setupToolingModelBuilder(project: Project) {
        toolingModelBuilderRegistry.register(project.pm20Extension.ideaKotlinProjectModelBuilder)
        project.locateOrRegisterBuildIdeaKotlinProjectModelTask()
    }
}

fun rootPublicationComponentName(module: KpmGradleModule) =
    module.disambiguateName("root")

open class KotlinPm20ProjectExtension(project: Project) : KotlinTopLevelExtension(project) {

    internal val kpmModelContainer = DefaultKpmGradleProjectModelContainer.create(project)

    internal val ideaKotlinProjectModelBuilder by lazy { IdeaKotlinProjectModelBuilder.default(this) }

    val modules: NamedDomainObjectContainer<KpmGradleModule>
        get() = project.kpmModules

    @Suppress("unused") // DSL function
    fun mainAndTest(configure: KpmGradleModule.() -> Unit) {
        main(configure)
        test(configure)
    }

    val main: KpmGradleModule
        get() = modules.getByName(KpmGradleModule.MAIN_MODULE_NAME)

    val test: KpmGradleModule
        get() = modules.getByName(KpmGradleModule.TEST_MODULE_NAME)

    fun main(configure: KpmGradleModule.() -> Unit = { }) = main.apply(configure)
    fun test(configure: KpmGradleModule.() -> Unit = { }) = test.apply(configure)

    @PublishedApi
    @JvmName("isAllowCommonizer")
    internal fun isAllowCommonizerForIde(@Suppress("UNUSED_PARAMETER") project: Project): Boolean = false
}

val KpmGradleModule.jvm: KpmJvmVariant
    get() = fragments.maybeCreate("jvm", KpmJvmVariant::class.java)

fun KpmGradleModule.jvm(configure: KpmJvmVariant.() -> Unit): KpmJvmVariant = jvm.apply(configure)

fun KotlinPm20ProjectExtension.jvm(configure: KotlinFragmentSlice<KpmJvmVariant>.() -> Unit) {
    val getOrCreateVariant: KpmGradleModule.() -> KpmJvmVariant = { jvm }
    mainAndTest { getOrCreateVariant(this) }
    val slice = KotlinFragmentSlice(this, getOrCreateVariant)
    configure(slice)
}

open class KotlinFragmentSlice<T : KpmGradleFragment>(
    val pm20ProjectExtension: KotlinPm20ProjectExtension,
    val getOrCreateFragment: (KpmGradleModule) -> T
) {
    fun inMain(configure: T.() -> Unit) {
        pm20ProjectExtension.modules.getByName(KpmGradleModule.MAIN_MODULE_NAME).apply {
            getOrCreateFragment(this).configure()
        }
    }

    fun inTest(configure: T.() -> Unit) {
        pm20ProjectExtension.modules.getByName(KpmGradleModule.TEST_MODULE_NAME).apply {
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

    fun inModule(module: NamedDomainObjectProvider<KpmGradleModule>, configure: T.() -> Unit) {
        module.get().apply { inModule(this, configure) }
    }

    fun inModule(module: KpmGradleModule, configure: T.() -> Unit) {
        getOrCreateFragment(module).configure()
    }
}
