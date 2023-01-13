/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.internal.isNativeDependencyPropagationEnabled
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.enableHierarchicalStructureByDefault
import kotlin.test.Test
import kotlin.test.assertEquals

class IdeImportPropertiesConsistencyTest {

    /*
    IDE IMPORT SOURCE CODE:
    https://youtrack.jetbrains.com/issue/KTIJ-19551

    As described in the ticket above, we have a separate implementation running in the IDE import that
    will resolve properties on the project. Unfortunately, this implementation will choose its own defaults.
    Changing defaults for such properties therefore can't be done easily in the KGP, without taking
    extra precaution ensuring that the IDE plugin would still receive the correct property.

    Until the implementation is changed and IDEs with old implementation are out of support,
    this code is copied into this test here and checked for consistency!
     */
    private fun Project.getProperty(property: GradleImportProperties): Boolean {
        val explicitValueIfAny = try {
            (findProperty(property.id) as? String)?.toBoolean()
        } catch (e: Exception) {
            logger.error("Error while trying to read property $property from project $project", e)
            null
        }

        return explicitValueIfAny ?: property.defaultValue
    }

    private enum class GradleImportProperties(val id: String, val defaultValue: Boolean) {
        IS_HMPP_ENABLED("kotlin.mpp.enableGranularSourceSetsMetadata", false),
        ENABLE_NATIVE_DEPENDENCY_PROPAGATION("kotlin.native.enableDependencyPropagation", true),
        /* IDE only relevant cases omitted */
        ;
    }

    /*
    END OF IDE IMPORT SOURCE CODE!
     */

    @Test
    fun `test simple project`() {
        val project = ProjectBuilder.builder().build()
        project.applyMultiplatformPlugin()
        project.assertPropertiesMatch()
    }

    @Test
    fun `test simple project with new hmpp flag`() {
        val project = ProjectBuilder.builder().build()
        project.enableHierarchicalStructureByDefault()
        project.applyMultiplatformPlugin()
        project.assertPropertiesMatch()
    }

    @Test
    fun `test sub project with new hmpp flag`() {
        val rootProject = ProjectBuilder.builder().build()
        val project = ProjectBuilder.builder().withParent(rootProject).build()
        rootProject.enableHierarchicalStructureByDefault()
        project.applyMultiplatformPlugin()
        project.assertPropertiesMatch()
    }

    private fun Project.assertPropertiesMatch() {
        val isHmppValueUsedInCli = project.isKotlinGranularMetadataEnabled
        val isHmppValueUsedInIde = project.getProperty(GradleImportProperties.IS_HMPP_ENABLED)
        assertEquals(
            isHmppValueUsedInCli, isHmppValueUsedInIde,
            """
                project.isKotlinGranularMetadataEnabled: $isHmppValueUsedInCli
                GradleImportProperties.IS_HMPP_ENABLED: $isHmppValueUsedInIde
            """.trimIndent()
        )

        val dependencyPropagationEnabledInCli = project.isNativeDependencyPropagationEnabled
        val dependencyPropagationEnabledInIde = project.getProperty(GradleImportProperties.ENABLE_NATIVE_DEPENDENCY_PROPAGATION)
        assertEquals(
            dependencyPropagationEnabledInCli, dependencyPropagationEnabledInIde,
            """
                propertiesProvider.nativeDependencyPropagation: $dependencyPropagationEnabledInCli
                GradleImportProperties.ENABLE_NATIVE_DEPENDENCY_PROPAGATION: $dependencyPropagationEnabledInIde
            """.trimIndent()
        )
    }
}
