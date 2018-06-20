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
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.createKotlinExtension
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSetProviderImpl
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.FileNotFoundException
import java.util.*
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class KotlinBasePluginWrapper(protected val fileResolver: FileResolver): Plugin<Project> {
    private val log = Logging.getLogger(this.javaClass)
    val kotlinPluginVersion = loadKotlinVersionFromResource(log)

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

        project.createKotlinExtension(projectExtensionClass)

        val plugin = getPlugin(kotlinGradleBuildServices)
        plugin.apply(project)
    }

    internal abstract fun getPlugin(kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project>

    open val projectExtensionClass: KClass<out KotlinProjectExtension> get() = KotlinProjectExtension::class
}

open class KotlinPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(kotlinGradleBuildServices: KotlinGradleBuildServices) =
            KotlinPlugin(KotlinTasksProvider(), KotlinSourceSetProviderImpl(fileResolver), kotlinPluginVersion)

    override val projectExtensionClass: KClass<out KotlinJvmProjectExtension>
        get() = KotlinJvmProjectExtension::class
}

open class KotlinCommonPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(kotlinGradleBuildServices: KotlinGradleBuildServices) =
            KotlinCommonPlugin(KotlinCommonTasksProvider(), KotlinSourceSetProviderImpl(fileResolver), kotlinPluginVersion)
}

open class KotlinAndroidPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(kotlinGradleBuildServices: KotlinGradleBuildServices) =
            KotlinAndroidPlugin(AndroidTasksProvider(), KotlinSourceSetProviderImpl(fileResolver), kotlinPluginVersion)
}

open class Kotlin2JsPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(kotlinGradleBuildServices: KotlinGradleBuildServices) =
            Kotlin2JsPlugin(Kotlin2JsTasksProvider(), KotlinSourceSetProviderImpl(fileResolver), kotlinPluginVersion)
}

fun Project.getKotlinPluginVersion(): String? {
    val kotlinPluginWrapper = plugins.findPlugin(KotlinAndroidPluginWrapper::class.java) ?: run {
        project.logger.error("'kotlin-android' plugin should be enabled before 'kotlin-android-extensions'")
        return null
    }

    return kotlinPluginWrapper.kotlinPluginVersion
}

private fun Any.loadKotlinVersionFromResource(log: Logger): String {
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
