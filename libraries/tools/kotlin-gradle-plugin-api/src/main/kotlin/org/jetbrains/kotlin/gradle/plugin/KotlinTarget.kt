/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.HasAttributes
import java.io.Serializable

interface KotlinTarget: HasAttributes {
    val targetName: String
    val disambiguationClassifier: String? get() = null

    val platformType: KotlinPlatformType

    val compilations: NamedDomainObjectContainer<out KotlinCompilation>
    val project: Project

    val defaultConfigurationName: String
    val apiElementsConfigurationName: String
    val runtimeElementsConfigurationName: String
}

enum class KotlinPlatformType: Named, Serializable {
    common, jvm, js,
    native; // TODO: split native into separate entries here or transform the enum to interface and implement entries in K/N

    override fun toString(): String = name
    override fun getName(): String = name
}