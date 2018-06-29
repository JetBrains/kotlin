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

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetContainer
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

internal val Project.kotlinExtension: KotlinProjectExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinProjectExtension

open class KotlinProjectExtension {
    val experimental: ExperimentalExtension
        get() = DslObject(this).extensions.getByType(ExperimentalExtension::class.java)

    val sourceSets: KotlinSourceSetContainer<out KotlinSourceSet>
        get() = DslObject(this).extensions.getByType(KotlinSourceSetContainer::class.java)

    val targets: KotlinTargetContainer
        get() = DslObject(this).extensions.getByType(KotlinTargetContainer::class.java)
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
