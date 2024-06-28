/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.getOrPut

internal fun handleHierarchicalStructureFlagsMigration(project: Project) {
    SingleActionPerProject.run(project.rootProject, "handleHierarchicalStructureFlagsMigration - rootProject") {
        doHandleHierarchicalStructureFlagsMigration(project.rootProject)
    }

    // rootProject will be handled with the SingleActionPerProject above
    if (project.rootProject !== project) {
        doHandleHierarchicalStructureFlagsMigration(project)
    }
}

private fun doHandleHierarchicalStructureFlagsMigration(project: Project) {
    with(PropertiesProvider(project)) {
        if (hierarchicalStructureSupport) {
            if (project === project.rootProject)
                project.extensions.extraProperties.getOrPut(PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA) { "true" }
            PropertiesProvider(project).mpp13XFlagsSetByPlugin = true
        }
    }
}
