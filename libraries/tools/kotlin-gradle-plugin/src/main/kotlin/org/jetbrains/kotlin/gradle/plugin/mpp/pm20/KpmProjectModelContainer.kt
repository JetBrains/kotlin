/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier

internal interface KpmGradleProjectModelContainer {
    val modules: NamedDomainObjectContainer<KotlinGradleModule>
    val metadataCompilationRegistryByModuleId: MutableMap<KotlinModuleIdentifier, MetadataCompilationRegistry>
    val rootPublication: MavenPublication?
}

internal val Project.kpmModelContainer: KpmGradleProjectModelContainer
    get() = kpmModelContainerOrNull ?: error("Couldn't find KPM container for $project")

internal val Project.kpmModelContainerOrNull: KpmGradleProjectModelContainer?
    get() = when (val ext = project.topLevelExtensionOrNull) {
        is KotlinPm20ProjectExtension -> ext.kpmModelContainer
        is KotlinProjectExtension -> if (PropertiesProvider(project).experimentalKpmModelMapping) ext.kpmModelContainer else null
        else -> null
    }

internal val Project.hasKpmModel: Boolean
    get() = kpmModelContainerOrNull != null

internal val Project.kpmModules: NamedDomainObjectContainer<KotlinGradleModule>
    get() = kpmModelContainer.modules

internal val Project.metadataCompilationRegistryByModuleId: MutableMap<KotlinModuleIdentifier, MetadataCompilationRegistry>
    get() = kpmModelContainer.metadataCompilationRegistryByModuleId

internal class DefaultKpmGradleProjectModelContainer(
    override val modules: NamedDomainObjectContainer<KotlinGradleModule>,
    override val metadataCompilationRegistryByModuleId: MutableMap<KotlinModuleIdentifier, MetadataCompilationRegistry>,
) : KpmGradleProjectModelContainer {
    override var rootPublication: MavenPublication? = null

    companion object {
        fun create(project: Project): DefaultKpmGradleProjectModelContainer {
            return DefaultKpmGradleProjectModelContainer(createKpmModulesContainer(project), mutableMapOf())
        }

        private fun createKpmModulesContainer(project: Project): NamedDomainObjectContainer<KotlinGradleModule> =
            project.objects.domainObjectContainer(
                KotlinGradleModule::class.java,
                KotlinGradleModuleFactory(project)
            )
    }
}