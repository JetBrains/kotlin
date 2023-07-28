/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import kotlin.test.Test
import kotlin.test.assertEquals

class IdeImportPropertiesConsistencyTest {

    /*
    Since 233+ IDEA even bundled kotlin plugin do not relying on the "kotlin.mpp.enableGranularSourceSetsMetadata" property
    and HMPP enabled in IDE by default. But for previous IDE versions with bundled Kotlin plugin we have the code that
    will disable the HMPP if this property is not set to true.

    To mitigate that we should pass the "kotlin.mpp.enableGranularSourceSetsMetadata=true" for cases where KGP 1.9.20+ trying to
    open in the old IDE.
    This compatibility trick could be safely removed in 2.1, but probably we should keep it in 2.0.

    Related issue: https://youtrack.jetbrains.com/issue/KTIJ-19551
     */
    private fun Project.getProperty(propertyId: String): Boolean? = try {
        (findProperty(propertyId) as? String)?.toBoolean()
    } catch (e: Exception) {
        logger.error("Error while trying to read property $propertyId from project $project", e)
        null
    }

    @Test
    fun `test simple project`() {
        val project = ProjectBuilder.builder().build()
        project.applyMultiplatformPlugin()
        project.assertKotlinGranularMetadataEnabled()
    }

    @Test
    fun `test HMPP enabled for all projects`() {
        val rootProject = ProjectBuilder.builder().build()
        val project = ProjectBuilder.builder().withParent(rootProject).build()
        project.applyMultiplatformPlugin()
        project.assertKotlinGranularMetadataEnabled()

        rootProject.assertKotlinGranularMetadataEnabled()
    }

    private fun Project.assertKotlinGranularMetadataEnabled() {
        val granularMetadataEnabled = project.getProperty("kotlin.mpp.enableGranularSourceSetsMetadata")
        assertEquals(
            true, granularMetadataEnabled,
            "kotlin.mpp.enableGranularSourceSetsMetadata: $granularMetadataEnabled"
        )
    }
}
