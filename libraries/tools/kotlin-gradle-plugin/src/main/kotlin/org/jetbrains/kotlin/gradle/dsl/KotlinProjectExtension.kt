/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.plugin.KotlinCommonSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.base.KotlinOnlyPlatformConfigurator
import org.jetbrains.kotlin.gradle.plugin.registerKotlinSourceSetsIfAbsent
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCommonTasksProvider
import org.jetbrains.kotlin.gradle.utils.matchSymmetricallyByNames
import kotlin.reflect.KClass

private const val KOTLIN_PROJECT_EXTENSION_NAME = "kotlin"

internal fun Project.createKotlinExtension(extensionClass: KClass<out KotlinProjectExtension>) {
    val kotlinExt = extensions.create(KOTLIN_PROJECT_EXTENSION_NAME, extensionClass.java)
    DslObject(kotlinExt).extensions.create("experimental", ExperimentalExtension::class.java)
}

internal val Project.kotlinExtension: KotlinPlatformExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinPlatformExtension

open class KotlinProjectExtension {
    val experimental: ExperimentalExtension
        get() = DslObject(this).extensions.getByType(ExperimentalExtension::class.java)
}

interface KotlinPlatformExtension {
    val platformName: String
    val platformDisambiguationClassifier: String? get() = null

    val sourceSets: KotlinSourceSetContainer<out KotlinSourceSet>
        get() = DslObject(this).extensions.getByType(KotlinSourceSetContainer::class.java)
}

internal fun KotlinPlatformExtension.disambiguateName(simpleName: String) =
    platformDisambiguationClassifier?.plus(simpleName.capitalize()) ?: simpleName

open class KotlinAndroidPlatformExtension : KotlinProjectExtension(), KotlinPlatformExtension {
    override val platformName: String = "kotlin"
}

open class KotlinJvmPlatformExtension : KotlinProjectExtension(), KotlinPlatformExtension {
    override val platformName: String get() = "kotlin"
    /**
     * With Gradle 4.0+, disables the separate output directory for Kotlin, falling back to sharing the deprecated
     * single classes directory per source set. With Gradle < 4.0, has no effect.
     * */
    var copyClassesToJavaOutput = false

    override val sourceSets: KotlinJavaSourceSetContainer
        get() = DslObject(this).extensions.getByType(KotlinJavaSourceSetContainer::class.java)
}

open class KotlinOnlyPlatformExtension: KotlinProjectExtension(), KotlinPlatformExtension {
    /** A non-null value if all entities connected to this extension, such as configurations, should contain the
     * platform classifier in their names. Null otherwise. */
    override lateinit var platformName: String
        internal set

    override var platformDisambiguationClassifier: String? = null
        internal set

    override val sourceSets: KotlinOnlySourceSetContainer
        get() = DslObject(this).extensions.getByType(KotlinOnlySourceSetContainer::class.java)
}

open class KotlinMultiplatformExtension : KotlinProjectExtension() {
    private val platformExtensionsByPlatformClassifier: MutableMap<String, KotlinPlatformExtension> = mutableMapOf()

    internal lateinit var project: Project
    internal lateinit var fileResolver: FileResolver
    internal lateinit var instantiator: Instantiator
    internal lateinit var buildOutputCleanupRegistry: BuildOutputCleanupRegistry
    internal lateinit var objectFactory: ObjectFactory

    private inline fun <reified T : KotlinPlatformExtension> configurePlatformExtensionByClassifier(
        classifier: String,
        crossinline createExtensionIfAbsent: () -> T,
        configureAction: T.() -> Unit
    ) {
        val extension = platformExtensionsByPlatformClassifier.computeIfAbsent(classifier) { createExtensionIfAbsent() } as T
        configureAction(extension)
    }

    private inline fun <reified T : KotlinSourceSet> configureSourceSetDefaults(
        extension: KotlinPlatformExtension,
        crossinline buildSourceSetProcessor: (T) -> KotlinSourceSetProcessor<*>
    ) {
        extension.sourceSets.all { sourceSet ->
            sourceSet as T
            buildSourceSetProcessor(sourceSet).run()
        }
    }

    fun common(configure: KotlinOnlyPlatformExtension.() -> Unit) {
        fun createExtension(): KotlinOnlyPlatformExtension {
            val extension = KotlinOnlyPlatformExtension().apply {
                platformName = "kotlinCommon"
                platformDisambiguationClassifier = "common"
            }
            val sourceSetContainer = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
            val tasksProvider = KotlinCommonTasksProvider()
            registerKotlinSourceSetsIfAbsent(sourceSetContainer, extension)
            KotlinOnlyPlatformConfigurator(buildOutputCleanupRegistry, objectFactory)
            configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
                KotlinCommonSourceSetProcessor(project, sourceSet, tasksProvider, sourceSetContainer)
            }
            return extension
        }

        configurePlatformExtensionByClassifier("common", ::createExtension, configure)
    }


    fun withJava(configure: KotlinJvmPlatformExtension.() -> Unit) {

    }

    fun jvm(configure: KotlinJvmPlatformExtension.() -> Unit) {

    }

    fun js(configure: KotlinOnlyPlatformExtension.() -> Unit) {
        fun createExtension(): KotlinOnlyPlatformExtension {
            val extension = KotlinOnlyPlatformExtension().apply {
                platformName = "kotlin2Js"
                platformDisambiguationClassifier = "js"
            }
            val sourceSetContainer = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
            val tasksProvider = KotlinCommonTasksProvider()
            registerKotlinSourceSetsIfAbsent(sourceSetContainer, extension)
            configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
                KotlinCommonSourceSetProcessor(project, sourceSet, tasksProvider, sourceSetContainer)
            }

            common {
                matchSymmetricallyByNames(this@common.sourceSets, sourceSetContainer) { commonSourceSet: KotlinSourceSet, _ ->
                    addCommonSourceSetToPlatformSourceSet(project, commonSourceSet)
                }
            }

            return extension
        }

        configurePlatformExtensionByClassifier("js", ::createExtension, configure)
    }

    protected open fun addCommonSourceSetToPlatformSourceSet(
        project: Project,
        commonSourceSet: KotlinSourceSet
    ) {
        val platformTask = project.tasks
            .filterIsInstance<AbstractKotlinCompile<*>>()
            .firstOrNull { it.sourceSetName == commonSourceSet.name }

        platformTask?.source(commonSourceSet.kotlin)
    }
}

open class ExperimentalExtension {
    var coroutines: Coroutines? = null
}

enum class Coroutines {
    ENABLE,
    WARN,
    ERROR;

    companion object {
        val DEFAULT = WARN

        fun byCompilerArgument(argument: String): Coroutines? =
                Coroutines.values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
    }
}
