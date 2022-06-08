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
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.cinterop.GradleKpmCinteropModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.cinterop.GradleKpmCinteropModuleFactory
import org.jetbrains.kotlin.project.model.KpmModuleIdentifier

internal interface GradleKpmProjectModelContainer {
    val modules: NamedDomainObjectContainer<GradleKpmModule>
    val cinteropModules: NamedDomainObjectContainer<GradleKpmCinteropModule>
    val metadataCompilationRegistryByModuleId: MutableMap<KpmModuleIdentifier, MetadataCompilationRegistry>
    val rootPublication: MavenPublication?
}

internal val Project.kpmModelContainer: GradleKpmProjectModelContainer
    get() = kpmModelContainerOrNull ?: error("Couldn't find KPM container for $project")

internal val Project.kpmModelContainerOrNull: GradleKpmProjectModelContainer?
    get() = when (val ext = project.topLevelExtensionOrNull) {
        is KotlinPm20ProjectExtension -> ext.kpmModelContainer
        is KotlinProjectExtension -> if (PropertiesProvider(project).experimentalKpmModelMapping) ext.kpmModelContainer else null
        else -> null
    }

internal val Project.hasKpmModel: Boolean
    get() = kpmModelContainerOrNull != null

internal val Project.kpmModules: NamedDomainObjectContainer<GradleKpmModule>
    get() = kpmModelContainer.modules

internal val Project.metadataCompilationRegistryByModuleId: MutableMap<KpmModuleIdentifier, MetadataCompilationRegistry>
    get() = kpmModelContainer.metadataCompilationRegistryByModuleId

internal class GradleKpmDefaultProjectModelContainer(
    override val modules: NamedDomainObjectContainer<GradleKpmModule>,
    override val cinteropModules: NamedDomainObjectContainer<GradleKpmCinteropModule>,
    override val metadataCompilationRegistryByModuleId: MutableMap<KpmModuleIdentifier, MetadataCompilationRegistry>,
) : GradleKpmProjectModelContainer {
    override var rootPublication: MavenPublication? = null

    companion object {
        fun create(project: Project) = GradleKpmDefaultProjectModelContainer(
            project.objects.domainObjectContainer(GradleKpmModule::class.java, GradleKpmModuleFactory(project)),
            project.objects.domainObjectContainer(GradleKpmCinteropModule::class.java, GradleKpmCinteropModuleFactory(project)),
            mutableMapOf()
        )
    }
}
