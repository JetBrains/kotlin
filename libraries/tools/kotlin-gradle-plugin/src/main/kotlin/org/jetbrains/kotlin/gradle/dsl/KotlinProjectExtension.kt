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

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import kotlin.reflect.KClass

private const val KOTLIN_PROJECT_EXTENSION_NAME = "kotlin"

internal fun Project.createKotlinExtension(extensionClass: KClass<out KotlinProjectExtension>): KotlinProjectExtension {
    val kotlinExt = extensions.create(KOTLIN_PROJECT_EXTENSION_NAME, extensionClass.java)
    DslObject(kotlinExt).extensions.create("experimental", ExperimentalExtension::class.java)
    return kotlinExtension
}

internal val Project.kotlinExtensionOrNull: KotlinProjectExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME) as? KotlinProjectExtension

internal val Project.kotlinExtension: KotlinProjectExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinProjectExtension

internal val Project.multiplatformExtensionOrNull: KotlinMultiplatformExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME) as? KotlinMultiplatformExtension

internal val Project.multiplatformExtension: KotlinMultiplatformExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinMultiplatformExtension

open class KotlinProjectExtension: KotlinSourceSetContainer {
    val experimental: ExperimentalExtension
        get() = DslObject(this).extensions.getByType(ExperimentalExtension::class.java)

    override var sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
        @Suppress("UNCHECKED_CAST")
        get() = DslObject(this).extensions.getByName("sourceSets") as NamedDomainObjectContainer<KotlinSourceSet>
        internal set(value) {
            DslObject(this).extensions.add("sourceSets", value)
        }
}

abstract class KotlinSingleTargetExtension : KotlinProjectExtension() {
    abstract val target: KotlinTarget

    open fun target(body: Closure<out KotlinTarget>) = ConfigureUtil.configure(body, target)
}

abstract class KotlinSingleJavaTargetExtension : KotlinSingleTargetExtension() {
    override abstract val target: KotlinWithJavaTarget<*>
}

open class KotlinJvmProjectExtension : KotlinSingleJavaTargetExtension() {
    override lateinit var target: KotlinWithJavaTarget<KotlinJvmOptions>
        internal set

    open fun target(body: KotlinWithJavaTarget<KotlinJvmOptions>.() -> Unit) = target.run(body)
}

open class Kotlin2JsProjectExtension : KotlinSingleJavaTargetExtension() {
    override lateinit var target: KotlinWithJavaTarget<KotlinJsOptions>
        internal set

    open fun target(body: KotlinWithJavaTarget<KotlinJsOptions>.() -> Unit) = target.run(body)
}

open class KotlinJsProjectExtension : KotlinSingleTargetExtension() {
    override lateinit var target: KotlinJsTarget

    open fun target(body: KotlinJsTarget.() -> Unit) = target.run(body)

    @Deprecated(
        "Needed for IDE import using the MPP import mechanism",
        level = DeprecationLevel.HIDDEN
    )
    fun getTargets() =
        target.project.container(KotlinTarget::class.java).apply { add(target) }
}

open class KotlinCommonProjectExtension : KotlinSingleJavaTargetExtension() {
    override lateinit var target: KotlinWithJavaTarget<KotlinMultiplatformCommonOptions>
        internal set

    open fun target(body: KotlinWithJavaTarget<KotlinMultiplatformCommonOptions>.() -> Unit) = target.run(body)
}

open class KotlinAndroidProjectExtension : KotlinSingleTargetExtension() {
    override lateinit var target: KotlinAndroidTarget
        internal set

    open fun target(body: KotlinAndroidTarget.() -> Unit) = target.run(body)
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
