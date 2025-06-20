package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableCrossCompilation
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test
import kotlin.test.assertEquals

class MetadataDependencyTransformationTaskInputsTest {

    @Test
    fun `metadata transformation - resolves host specific metadata only in compilations where host specific metadata is published`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                enableCrossCompilation()
            }
        ) {
            kotlin {
                iosArm64()
                iosX64()
                linuxArm64()
                linuxX64()
                jvm()
            }
        }.evaluate()

        assertEquals(
            listOf(
                project.multiplatformExtension.iosArm64().compilations.getByName("main")
                    .internal.configurations.hostSpecificMetadataConfiguration,
                project.multiplatformExtension.iosX64().compilations.getByName("main")
                    .internal.configurations.hostSpecificMetadataConfiguration,
            ),
            project.locateOrRegisterMetadataDependencyTransformationTask(
                project.multiplatformExtension.sourceSets.getByName("commonMain")
            ).get().taskInputs.hostSpecificMetadataConfigurations
        )
    }

}