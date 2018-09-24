/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_COMPILER_EMBEDDABLE
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import java.io.FileNotFoundException
import java.util.*
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class KotlinBasePluginWrapper(
    protected val fileResolver: FileResolver
): Plugin<Project> {
    private val log = Logging.getLogger(this.javaClass)
    val kotlinPluginVersion = loadKotlinVersionFromResource(log)

    open val projectExtensionClass: KClass<out KotlinProjectExtension> get() = KotlinProjectExtension::class

    internal open fun kotlinSourceSetFactory(project: Project): KotlinSourceSetFactory<out KotlinSourceSet> =
        DefaultKotlinSourceSetFactory(project, fileResolver)

    override fun apply(project: Project) {
        project.configurations.maybeCreate(COMPILER_CLASSPATH_CONFIGURATION_NAME).defaultDependencies {
            it.add(project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:$kotlinPluginVersion"))
        }
        project.configurations.maybeCreate(PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            // todo: Consider removing if org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser stops using parent last classloader
            isTransitive = false
        }

        // TODO: consider only set if if daemon or parallel compilation are enabled, though this way it should be safe too
        System.setProperty(org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")
        val kotlinGradleBuildServices = KotlinGradleBuildServices.getInstance(project.gradle)

        project.createKotlinExtension(projectExtensionClass).apply {
            fun <T : KotlinSourceSet> kotlinSourceSetContainer(factory: KotlinSourceSetFactory<T>) =
                project.container(factory.itemClass, factory)

            project.kotlinExtension.sourceSets = kotlinSourceSetContainer(kotlinSourceSetFactory(project))
        }

        val plugin = getPlugin(project, kotlinGradleBuildServices)

        setupAttributeMatchingStrategy(project)

        plugin.apply(project)
    }

    private fun setupAttributeMatchingStrategy(project: Project) = with(project.dependencies.attributesSchema) {
        attribute(KotlinPlatformType.attribute).run {
            if (isGradleVersionAtLeast(4, 0)) {
                compatibilityRules.add(KotlinPlatformType.CompatibilityRule::class.java)
            }
        }
    }

    internal abstract fun getPlugin(
        project: Project,
        kotlinGradleBuildServices: KotlinGradleBuildServices
    ): Plugin<Project>
}

open class KotlinPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    protected val registry: ToolingModelBuilderRegistry
): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
            KotlinPlugin(kotlinPluginVersion, registry)

    override val projectExtensionClass: KClass<out KotlinJvmProjectExtension>
        get() = KotlinJvmProjectExtension::class
}

open class KotlinCommonPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    protected val registry: ToolingModelBuilderRegistry
): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
            KotlinCommonPlugin(kotlinPluginVersion, registry)

    override val projectExtensionClass: KClass<out KotlinSingleJavaTargetExtension>
        get() = KotlinSingleJavaTargetExtension::class
}

open class KotlinAndroidPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    protected val registry: ToolingModelBuilderRegistry
): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinAndroidPlugin(kotlinPluginVersion)
}

open class Kotlin2JsPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    protected val registry: ToolingModelBuilderRegistry
): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        Kotlin2JsPlugin(kotlinPluginVersion, registry)

    override val projectExtensionClass: KClass<out KotlinSingleJavaTargetExtension>
        get() = KotlinSingleJavaTargetExtension::class
}

open class KotlinMultiplatformPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    private val instantiator: Instantiator,
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry
): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinMultiplatformPlugin(
            buildOutputCleanupRegistry, fileResolver,
            instantiator, kotlinPluginVersion
        )

    override val projectExtensionClass: KClass<out KotlinMultiplatformExtension>
        get() = KotlinMultiplatformExtension::class
}

fun Project.getKotlinPluginVersion(): String? =
    plugins.asSequence().mapNotNull { (it as? KotlinBasePluginWrapper)?.kotlinPluginVersion }.firstOrNull()

fun Plugin<*>.loadKotlinVersionFromResource(log: Logger): String {
    log.kotlinDebug("Loading version information")
    val props = Properties()
    val propFileName = "project.properties"
    val inputStream = javaClass.classLoader!!.getResourceAsStream(propFileName) ?:
            throw FileNotFoundException("property file '$propFileName' not found in the classpath")

    props.load(inputStream)

    val projectVersion = props["project.version"] as String
    log.kotlinDebug("Found project version [$projectVersion]")
    return projectVersion
}
