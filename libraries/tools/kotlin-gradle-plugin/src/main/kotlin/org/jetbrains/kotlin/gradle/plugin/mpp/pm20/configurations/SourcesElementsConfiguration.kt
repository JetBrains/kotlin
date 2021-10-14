/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configurations

import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.ProtoConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.taskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.tasks.SourcesJarTask
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.usageByName

object SourcesElementsConfiguration : ProtoConfiguration {
    override fun registerInProject(project: Project) {
        project.pm20Extension.modules.all { module ->
            project.configurations.create(nameIn(module)).apply {
                isCanBeResolved = false
                isCanBeConsumed = true

                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_SOURCES))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, Category.DOCUMENTATION))
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.EXTERNAL))
                attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType::class.java, DocsType.SOURCES))

                outgoing.artifact(module.taskProvider(SourcesJarTask).get()) {
                    it.classifier = "sources"
                }

                ComputedCapability.fromModuleOrNull(module)?.let {
                    outgoing.capability(it)
                }
            }
        }
    }

    override fun nameIn(module: KotlinGradleModule): String = module.disambiguateName("sourceElements")
}
