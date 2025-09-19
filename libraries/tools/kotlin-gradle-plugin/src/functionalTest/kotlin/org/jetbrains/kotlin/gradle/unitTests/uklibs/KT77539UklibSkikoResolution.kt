package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.util.assertContains
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableDefaultJsDomApiDependency
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.setUklibPublicationStrategy
import org.jetbrains.kotlin.gradle.util.setUklibResolutionStrategy
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KT77539UklibSkikoResolution {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun test() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
                setUklibResolutionStrategy(KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs)
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            }
        ) {
            repositories.mavenCentral()
            kotlin {
                jvm()
                val skiko = dependencies.create("org.jetbrains.skiko:skiko:0.9.4.2") {
                    isTransitive = false
                }
                dependencies {
                    implementation.add(skiko)
                }
            }
        }.evaluate()
        val skiko = project.configurations.getByName("jvmCompileClasspath")
            .incoming.resolutionResult.allDependencies.single() as UnresolvedDependencyResult
        assertContains(
            """
                  - androidApiElements-published
                  - awtApiElements-published
                  - metadataApiElements
                All of them match the consumer attributes:
            """.trimIndent(),
            skiko.failure.stackTraceToString()
        )
    }
}