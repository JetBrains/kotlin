package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.setUklibResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.setUklibPublicationStrategy
import org.junit.Test
import kotlin.test.assertEquals

class UklibTaskDependencyTests {

    @Test
    fun `assemble task dependencies with enabled uklibs`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                setUklibResolutionStrategy(KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs)
                setUklibPublicationStrategy(KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication)
            }
        ) {
            kotlin {
                jvm()
            }
        }.evaluate()
        val assembleDependencies = project.tasks.getByName("assemble").taskDependencies.getDependencies(null).map { it.name }.toSet()
        assertEquals(
            setOf<String>(
                "allMetadataJar",
                "archiveUklib",
                "jvmJar",
            ).prettyPrinted,
            assembleDependencies.prettyPrinted,
        )
    }
}