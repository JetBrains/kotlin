package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class KT77539UklibSkikoResolution {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** See [org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.workaroundLegacySkikoResolutionKT77539] */
    @Test
    fun `skiko resolution doesn't explode because of hacky android variant`() {
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
        val skikoCompilationDependency = project.configurations.getByName("jvmCompileClasspath")
            .incoming.resolutionResult.allComponents.map {
                (it as ResolvedComponentResult).variants.single().displayName
            }
        val skikoRuntimeDependency = project.configurations.getByName("jvmRuntimeClasspath")
            .incoming.resolutionResult.allComponents.map {
                (it as ResolvedComponentResult).variants.single().displayName
            }
        assertEquals(
            listOf("jvmCompileClasspath", "awtApiElements-published", "awtApiElements-published"),
            skikoCompilationDependency,
        )
        assertEquals(
            listOf("jvmRuntimeClasspath", "awtRuntimeElements-published", "awtRuntimeElements-published"),
            skikoRuntimeDependency,
        )
    }
}