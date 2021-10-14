/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configurations

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.ProtoConfiguration
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

object ResolvableMetadataConfiguration : ProtoConfiguration {
    override fun registerInProject(project: Project) {
        project.pm20Extension.modules.all { module ->
            project.configurations.create(nameIn(module)).apply {
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
                module.fragments.all { fragment ->
                    project.addExtendsFromRelation(name, fragment.apiConfigurationName)
                    project.addExtendsFromRelation(name, fragment.implementationConfigurationName)
                }
            }
        }
    }

    override fun nameIn(module: KotlinGradleModule): String = lowerCamelCaseName(module.name, "DependenciesMetadata")
}
