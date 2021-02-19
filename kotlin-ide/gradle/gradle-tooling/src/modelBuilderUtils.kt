/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task

private val kotlinSingleTargetExtensionClassNames = listOf(
        "org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension_Decorated",
        "org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension_Decorated",
        "org.jetbrains.kotlin.gradle.dsl.KotlinCommonProjectExtension_Decorated",
        "org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension_Decorated",
        "org.jetbrains.kotlin.gradle.dsl.Kotlin2JsProjectExtension_Decorated",
)

operator fun Any?.get(methodName: String, vararg params: Any): Any? {
    return this[methodName, params.map { it.javaClass }, params.toList()]
}

operator fun Any?.get(methodName: String, paramTypes: List<Class<*>>, params: List<Any?>): Any? {
    if (this == null) return null
    return this::class.java.getMethodOrNull(methodName, *paramTypes.toTypedArray())?.invoke(this, *params.toTypedArray())
}

fun Project.getTarget(): Named? = project.extensions.findByName("kotlin")?.let { kotlinExt ->
    if (kotlinExt.javaClass.name in kotlinSingleTargetExtensionClassNames) kotlinExt["getTarget"] as? Named
    else null
}

fun Project.getTargets(): Collection<Named>? = project.extensions.findByName("kotlin")?.let { kotlinExt ->
    @Suppress("UNCHECKED_CAST")
    (kotlinExt["getTargets"] as? NamedDomainObjectContainer<Named>)?.asMap?.values
}

@Suppress("UNCHECKED_CAST")
val Named.compilations: Collection<Named>?
    get() = (this["getCompilations"] as? NamedDomainObjectContainer<Named>)?.asMap?.values

fun Named.getCompileKotlinTaskName(project: Project): Task? = (this["getCompileKotlinTaskName"] as? String)?.let { project.tasks.findByName(it) }

