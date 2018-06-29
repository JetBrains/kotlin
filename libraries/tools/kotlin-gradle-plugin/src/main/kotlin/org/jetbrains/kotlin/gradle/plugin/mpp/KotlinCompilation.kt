/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal fun KotlinCompilation.composeName(prefix: String? = null, suffix: String? = null): String {
    val compilationName = (if (name == "main") "" else name).let {
        if (prefix.isNullOrEmpty()) it else it.capitalize()
    } + target.targetName.capitalize()
    val resultPrefix = (prefix ?: "") + compilationName
    val resultSuffix = (if (resultPrefix.isEmpty()) suffix else suffix?.capitalize()) ?: ""
    return resultPrefix + resultSuffix
}

abstract class AbstractKotlinCompilation(
    final override val target: KotlinTarget,
    override val name: String
) : KotlinCompilation {
    private val attributeContainer = HierarchyAttributeContainer(target.attributes)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val kotlinSourceSets: MutableList<KotlinSourceSet>
        get() = mutableListOf()

    override fun source(sourceSet: KotlinSourceSet) {
        kotlinSourceSets += sourceSet
    }

    override val output: SourceSetOutput by lazy { KotlinSourceSetOutput((target as AbstractKotlinTarget).project) }

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName("compile", name, "Kotlin", target.targetName)

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(name, target.targetName, "classes")

    override val apiDependencyFiles: FileCollection
        get() = TODO("not implemented")

    override val runtimeDependencyFiles: FileCollection
        get() = TODO("not implemented")
}

val KotlinCompilation.classesTaskName: String get() = "${name}Classes"

open class KotlinJvmCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilation(target, name)

class KotlinJvmWithJavaCompilation(
    target: KotlinTarget,
    name: String,
    val javaSourceSet: SourceSet
) : KotlinJvmCompilation(target, name) {
    override val output: SourceSetOutput
        get() = javaSourceSet.output
}

class KotlinJsCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilation(target, name)

class KotlinCommonCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilation(target, name)