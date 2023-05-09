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
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProjectModelBuilder
import org.jetbrains.kotlin.gradle.kpm.idea.default
import org.jetbrains.kotlin.gradle.kpm.idea.locateOrRegisterIdeaKpmBuildProjectModelTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
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
            modules.create(GradleKpmModule.MAIN_MODULE_NAME)
            modules.create(GradleKpmModule.TEST_MODULE_NAME)
            main { makePublic() }
        }
    }

    private fun setupPublicationForModule(module: GradleKpmModule) {
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
        toolingModelBuilderRegistry.register(project.pm20Extension.ideaKpmProjectModelBuilder)
        project.locateOrRegisterIdeaKpmBuildProjectModelTask()
    }
}

fun rootPublicationComponentName(module: GradleKpmModule) =
    module.disambiguateName("root")

// NB: inheriting `KotlinProjectExtension` is a hack, as well as overriding 'sourceSets'
// This is done because 'project.kotlinProjectExtension' casts extension to KotlinProjectExtension,
// resulting in CCE, and some code (like KotlinGradleProjectCheckers) that is launched universally
// in KPM and TCS actually calls this method, breaking KPM tests.
//
// Ideally, respective code should just treat top-level extensions more carefully, but as KPM
// is not in focus for now, we don't want to complicate the new code with those matters.
//
// Inheriting KotlinProjectExtension allows such code to not fail. When KPM work is resumed,
// this inheritance should be removed
open class KotlinPm20ProjectExtension(project: Project) : KotlinProjectExtension(project) {
    override var sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
        get() = super.sourceSets
        set(value) {
            super.sourceSets = value
        }

    internal val kpmModelContainer = GradleKpmDefaultProjectModelContainer.create(project)

    internal val ideaKpmProjectModelBuilder by lazy { IdeaKpmProjectModelBuilder.default(this) }

    val modules: NamedDomainObjectContainer<GradleKpmModule>
        get() = kpmModelContainer.modules

    @Suppress("unused") // DSL function
    fun mainAndTest(configure: GradleKpmModule.() -> Unit) {
        main(configure)
        test(configure)
    }

    val main: GradleKpmModule
        get() = modules.getByName(GradleKpmModule.MAIN_MODULE_NAME)

    val test: GradleKpmModule
        get() = modules.getByName(GradleKpmModule.TEST_MODULE_NAME)

    fun main(configure: GradleKpmModule.() -> Unit = { }) = main.apply(configure)
    fun test(configure: GradleKpmModule.() -> Unit = { }) = test.apply(configure)

    @PublishedApi
    @JvmName("isAllowCommonizer")
    internal fun isAllowCommonizerForIde(@Suppress("UNUSED_PARAMETER") project: Project): Boolean = false
}

val GradleKpmModule.jvm: GradleKpmJvmVariant
    get() = fragments.maybeCreate("jvm", GradleKpmJvmVariant::class.java)

fun GradleKpmModule.jvm(configure: GradleKpmJvmVariant.() -> Unit): GradleKpmJvmVariant = jvm.apply(configure)

fun KotlinPm20ProjectExtension.jvm(configure: KotlinFragmentSlice<GradleKpmJvmVariant>.() -> Unit) {
    val getOrCreateVariant: GradleKpmModule.() -> GradleKpmJvmVariant = { jvm }
    mainAndTest { getOrCreateVariant(this) }
    val slice = KotlinFragmentSlice(this, getOrCreateVariant)
    configure(slice)
}

open class KotlinFragmentSlice<T : GradleKpmFragment>(
    val pm20ProjectExtension: KotlinPm20ProjectExtension,
    val getOrCreateFragment: (GradleKpmModule) -> T
) {
    fun inMain(configure: T.() -> Unit) {
        pm20ProjectExtension.modules.getByName(GradleKpmModule.MAIN_MODULE_NAME).apply {
            getOrCreateFragment(this).configure()
        }
    }

    fun inTest(configure: T.() -> Unit) {
        pm20ProjectExtension.modules.getByName(GradleKpmModule.TEST_MODULE_NAME).apply {
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

    fun inModule(module: NamedDomainObjectProvider<GradleKpmModule>, configure: T.() -> Unit) {
        module.get().apply { inModule(this, configure) }
    }

    fun inModule(module: GradleKpmModule, configure: T.() -> Unit) {
        getOrCreateFragment(module).configure()
    }
}
