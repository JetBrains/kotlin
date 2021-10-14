/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configurations

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.setModuleCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.tasks.MetadataJarTask
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation

object MetadataElementsConfiguration : ProtoConfiguration {
    override fun registerInProject(project: Project) {
        project.pm20Extension.modules.all { module ->
            project.configurations.create(nameIn(module)) {
                it.isCanBeConsumed = false
                module.ifMadePublic {
                    it.isCanBeConsumed = true
                }

                it.isCanBeResolved = false

                project.artifacts.add(it.name, module.taskProvider(MetadataJarTask))

                it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
                it.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)

                module.fragments.all { fragment ->
                    // FIXME: native api-implementation
                    project.addExtendsFromRelation(it.name, fragment.apiConfigurationName)
                }

                setModuleCapability(it, module)
            }
        }
    }

    override fun nameIn(module: KotlinGradleModule): String = module.disambiguateName("metadataElements")
}
