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
import org.gradle.api.internal.plugins.DslObject
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinJavaSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinOnlySourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinSourceSetContainer
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

    val platformType: KotlinPlatformType

    val sourceSets: KotlinSourceSetContainer<out KotlinSourceSet>
        get() = DslObject(this).extensions.getByType(KotlinSourceSetContainer::class.java)
}

internal fun KotlinPlatformExtension.disambiguateName(simpleName: String) =
    platformDisambiguationClassifier?.plus(simpleName.capitalize()) ?: simpleName

open class KotlinAndroidPlatformExtension : KotlinProjectExtension(), KotlinPlatformExtension {
    override val platformName: String = "kotlin"
    override val platformType = KotlinPlatformType.JVM
}

open class KotlinWithJavaPlatformExtension : KotlinProjectExtension(), KotlinPlatformExtension {
    override val platformName: String = "kotlin"
    override val platformType = KotlinPlatformType.JVM
    /**
     * With Gradle 4.0+, disables the separate output directory for Kotlin, falling back to sharing the deprecated
     * single classes directory per source set. With Gradle < 4.0, has no effect.
     * */
    var copyClassesToJavaOutput = false

    override val sourceSets: KotlinJavaSourceSetContainer
        get() = DslObject(this).extensions.getByType(KotlinJavaSourceSetContainer::class.java)
}

open class KotlinOnlyPlatformExtension: KotlinProjectExtension(), KotlinPlatformExtension {
    override lateinit var platformType: KotlinPlatformType
        internal set

    override lateinit var platformName: String
        internal set

    /** A non-null value if all project-global entities connected to this extension, such as configurations, should contain the
     * platform classifier in their names. Null otherwise. */
    override var platformDisambiguationClassifier: String? = null
        internal set

    var userDefinedPlatformId: String? = null

    override val sourceSets: KotlinOnlySourceSetContainer
        get() = DslObject(this).extensions.getByType(KotlinOnlySourceSetContainer::class.java)
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
