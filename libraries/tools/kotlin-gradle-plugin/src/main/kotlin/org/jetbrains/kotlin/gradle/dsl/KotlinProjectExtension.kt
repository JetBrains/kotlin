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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import kotlin.reflect.KClass

private const val KOTLIN_PROJECT_EXTENSION_NAME = "kotlin"

internal fun Project.createKotlinExtension(extensionClass: KClass<out KotlinProjectExtension>): KotlinProjectExtension {
    val kotlinExt = extensions.create(KOTLIN_PROJECT_EXTENSION_NAME, extensionClass.java)
    DslObject(kotlinExt).extensions.create("experimental", ExperimentalExtension::class.java)
    return kotlinExtension
}

internal val Project.kotlinExtension: KotlinProjectExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinProjectExtension

open class KotlinProjectExtension {
    val experimental: ExperimentalExtension
        get() = DslObject(this).extensions.getByType(ExperimentalExtension::class.java)

    var sourceSets: NamedDomainObjectContainer<out KotlinSourceSet>
        @Suppress("UNCHECKED_CAST")
        get() = DslObject(this).extensions.getByName("sourceSets") as NamedDomainObjectContainer<out KotlinSourceSet>
        internal set(value) { DslObject(this).extensions.add("sourceSets", value) }
}

open class KotlinSingleJavaTargetExtension : KotlinProjectExtension() {
    internal lateinit var target: KotlinWithJavaTarget
}

open class KotlinJvmProjectExtension : KotlinSingleJavaTargetExtension() {
    /**
     * With Gradle 4.0+, disables the separate output directory for Kotlin, falling back to sharing the deprecated
     * single classes directory per source set. With Gradle < 4.0, has no effect.
     * */
    var copyClassesToJavaOutput = false
}

open class ExperimentalExtension {
    var coroutines: Coroutines? = null
}

enum class Coroutines {
    ENABLE,
    WARN,
    ERROR,
    DEFAULT;

    companion object {
        fun byCompilerArgument(argument: String): Coroutines? =
            Coroutines.values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
    }
}
