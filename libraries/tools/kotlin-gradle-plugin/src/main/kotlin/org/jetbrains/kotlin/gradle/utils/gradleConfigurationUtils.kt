/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

fun Project.addExtendsFromRelation(extendingConfigurationName: String, extendsFromConfigurationName: String, forced: Boolean = true) {
    if (extendingConfigurationName == extendsFromConfigurationName) return

    val extending = configurations.findByName(extendingConfigurationName)
        ?: if (forced) throw RuntimeException("Configuration $extendingConfigurationName does not exist.")
        else return

    extending.extendsFrom(configurations.getByName(extendsFromConfigurationName))
}

fun NamedDomainObjectProvider<Configuration>.extendsFrom(other: NamedDomainObjectProvider<Configuration>) {
    if (name == other.name) return
    configure { extending -> extending.extendsFrom(other.get()) }
}
