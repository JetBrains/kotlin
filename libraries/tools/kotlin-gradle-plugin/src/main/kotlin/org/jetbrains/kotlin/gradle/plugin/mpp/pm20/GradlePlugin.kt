/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.*
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier

abstract class KotlinPm20GradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        checkGradleCompatibility("the Kotlin Multiplatform plugin", GradleVersion.version("6.1"))

        // Gradle sets up the attribute schema for consuming JVM dependencies in the JavaBasePlugin
        project.plugins.apply(JavaBasePlugin::class.java)

        createDefaultModules(project)
        customizeKotlinDependencies(project)
        registerDefaultVariantFactories(project)
    }

    private fun registerDefaultVariantFactories(project: Project) {
        project.pm20Extension.modules.configureEach {
            it.fragments.registerFactory(
                KotlinJvmVariant::class.java,
                KotlinJvmVariantFactory(it)
            )
        }
    }

    private fun createDefaultModules(project: Project) {
        project.pm20Extension.apply {
            modules.create(KotlinGradleModule.MAIN_MODULE_NAME)
            modules.create(KotlinGradleModule.TEST_MODULE_NAME)
            main { makePublic() }
        }
        setupFragmentsMetadata(project)
    }

    private fun setupFragmentsMetadata(project: Project) {
        project.pm20Extension.modules.all { module ->
            configureMetadataResolutionAndBuild(module)
            module.ifMadePublic { configureMetadataExposure(module) }
        }
    }
}

open class KotlinPm20ProjectExtension(project: Project) : KotlinTopLevelExtension(project) {
    val modules: NamedDomainObjectContainer<KotlinGradleModule> by lazy {
        project.objects.domainObjectContainer(
            KotlinGradleModule::class.java,
            KotlinGradleModuleFactory(project)
        )
    }

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
}

val KotlinGradleModule.jvm: KotlinJvmVariant
    get() = fragments.maybeCreate("jvm", KotlinJvmVariant::class.java)

fun KotlinGradleModule.jvm(configure: KotlinJvmVariant.() -> Unit): KotlinJvmVariant = jvm.apply(configure)

fun KotlinPm20ProjectExtension.jvm(configure: KotlinFragmentSlice<KotlinJvmVariant>.() -> Unit) {
    val getOrCreateModule: KotlinGradleModule.() -> KotlinJvmVariant = { fragments.maybeCreate("jvm", KotlinJvmVariant::class.java) }
    mainAndTest { getOrCreateModule(this) }
    val slice = KotlinFragmentSlice(this, getOrCreateModule)
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