/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
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
        checkHmppFeatureFlagsForConsistency(project)

        if (hierarchicalStructureSupport) {
            if (project === project.rootProject)
                project.extensions.extraProperties.getOrPut(PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA) { "true" }
            project.extensions.extraProperties.getOrPut(PropertyNames.KOTLIN_NATIVE_DEPENDENCY_PROPAGATION) { "false" }
            PropertiesProvider(project).mpp13XFlagsSetByPlugin = true
        }
    }
}

private fun PropertiesProvider.checkHmppFeatureFlagsForConsistency(project: Project) {
    if (mppHierarchicalStructureByDefault) {
        if (hierarchicalStructureSupport) {
            if (project === project.rootProject) {
                when (enableGranularSourceSetsMetadata) {
                    true -> if (!mpp13XFlagsSetByPlugin) warnGranularMetadataTrueHasNoEffect(project)
                    false -> errorGranularMetadataFalseUnsupported()
                    null -> Unit
                }
            }
            when (nativeDependencyPropagation) {
                false -> if (!mpp13XFlagsSetByPlugin) warningDependencyPropagationFalseHasNoEffect(project)
                true -> errorDependencyPropagationTrueUnsupported()
                null -> Unit
            }
        } else { // hierarchicalStructureSupport is false
            if (project === project.rootProject) {
                when (enableGranularSourceSetsMetadata) {
                    true -> errorGranularMetadataTrueConflictsWithNewFlag()
                    false -> warningGranularMetadataFalseRedundantWithNewFlag(project)
                    null -> Unit
                }
            }
            if (!mpp13XFlagsSetByPlugin && nativeDependencyPropagation == false)
                errorDependencyPropagationFalseConflictsWithNewFlag()
        }
    } else { // mppHierarchicalStructureByDefault is false
        if (project.findProperty(PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT) != null)
            throw GradleException("The property '${PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT}' is not yet supported.")
    }
}

private fun errorDependencyPropagationFalseConflictsWithNewFlag() {
    throw GradleException(
        "Conflicting properties: '${PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT}=false' (enabling Kotlin/Native " +
                "dependencies commonization) " +
                "and '${PropertyNames.KOTLIN_NATIVE_DEPENDENCY_PROPAGATION}=false'"
    )
}

private fun warningGranularMetadataFalseRedundantWithNewFlag(project: Project) {
    SingleWarningPerBuild.show(
        project,
        "The property '${PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA}=false' is redundant with " +
                "'${PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT}=false'\n"
    )
}

private fun errorGranularMetadataTrueConflictsWithNewFlag() {
    throw GradleException(
        "Conflicting properties: '${PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT}=false' and " +
                "'${PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA}=true'."
    )
}

private fun warningDependencyPropagationFalseHasNoEffect(project: Project) {
    SingleWarningPerBuild.show(
        project,
        "The property '${PropertyNames.KOTLIN_NATIVE_DEPENDENCY_PROPAGATION}=false' has no effect in this and future " +
                "Kotlin versions, as Kotlin/Native dependency commonization is now enabled by default. " +
                "It is safe to remove the property.\n"
    )
}

private fun errorDependencyPropagationTrueUnsupported() {
    throw GradleException(
        "Kotlin/Native dependencies commonization is now enabled by default for all projects with Native-shared code, " +
                "and the property '${PropertyNames.KOTLIN_NATIVE_DEPENDENCY_PROPAGATION}=true' is not supported anymore. " +
                "It is possible to temporarily disable Hierarchical Structures support altogether with the following Gradle " +
                "property:\n\n" +
                "    ${PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT}=false\n\n" +
                "If you are facing any issues with shared code and Hierarchical Structures support, please report them " +
                "at https://kotl.in/issue\n"
    )
}

private fun errorGranularMetadataFalseUnsupported() {
    throw GradleException(
        "Kotlin Multiplatform Hierarchical Structures support is now enabled by default for all projects, and the " +
                "property '${PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA}=false' is not supported anymore. " +
                "If you need to temporarily revert to the old behavior, please use the following Gradle property:\n\n" +
                "    ${PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT}=false\n\n" +
                "If you are facing any issues with shared code and Hierarchical Structures support, please report them " +
                "at https://kotl.in/issue"
    )
}

private fun warnGranularMetadataTrueHasNoEffect(project: Project) {
    SingleWarningPerBuild.show(
        project,
        "The property '${PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA}=true' has no effect in this and future " +
                "Kotlin versions, as Hierarchical Structures support is now enabled by default. " +
                "It is safe to remove the property.\n"
    )
}
