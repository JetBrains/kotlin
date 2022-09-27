/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.project.model.KpmModuleIdentifier

internal interface GradleKpmProjectModelContainer {
    val modules: NamedDomainObjectContainer<GradleKpmModule>
    val metadataCompilationRegistryByModuleId: MutableMap<KpmModuleIdentifier, MetadataCompilationRegistry>
    val rootPublication: MavenPublication?
}

internal val Project.metadataCompilationRegistryByModuleId: MutableMap<KpmModuleIdentifier, MetadataCompilationRegistry>
    get() = this.pm20Extension.kpmModelContainer.metadataCompilationRegistryByModuleId

internal class GradleKpmDefaultProjectModelContainer(
    override val modules: NamedDomainObjectContainer<GradleKpmModule>,
    override val metadataCompilationRegistryByModuleId: MutableMap<KpmModuleIdentifier, MetadataCompilationRegistry>,
) : GradleKpmProjectModelContainer {
    override var rootPublication: MavenPublication? = null

    companion object {
        fun create(project: Project): GradleKpmDefaultProjectModelContainer {
            return GradleKpmDefaultProjectModelContainer(createKpmModulesContainer(project), mutableMapOf())
        }

        private fun createKpmModulesContainer(project: Project): NamedDomainObjectContainer<GradleKpmModule> =
            project.objects.domainObjectContainer(
                GradleKpmModule::class.java,
                GradleKpmModuleFactory(project)
            )
    }
}
