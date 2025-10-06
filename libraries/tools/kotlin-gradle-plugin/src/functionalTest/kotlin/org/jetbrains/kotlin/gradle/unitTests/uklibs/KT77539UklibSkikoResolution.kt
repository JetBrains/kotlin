package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.appleMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.plugin.sources.internal
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
                iosArm64()
                iosX64()
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

        project.configurations.getByName(
            project.multiplatformExtension.iosArm64().compilations.getByName("main")
                .compileDependencyConfigurationName
        ).resolve()

        assertEquals(
            listOf("jvmRuntimeClasspath", "awtRuntimeElements-published", "awtRuntimeElements-published"),
            skikoRuntimeDependency,
        )

        project.multiplatformExtension.sourceSets
            .commonMain.get().internal.resolvableMetadataConfiguration.resolve()
        val metadataResolution = project.multiplatformExtension.sourceSets
            .commonMain.get().internal.resolvableMetadataConfiguration.incoming.resolutionResult.allComponents.map {
                (it as ResolvedComponentResult).variants.single().displayName
            }
        assertEquals(
            listOf("commonMainResolvableDependenciesMetadata", "metadataApiElements"),
            metadataResolution
        )

        val hostSpecificMetadataConfiguration = project.multiplatformExtension.iosArm64().compilations.getByName("main").internal
            .configurations.hostSpecificMetadataConfiguration!!
        hostSpecificMetadataConfiguration.resolve()
        val hostSpecificMetadataResolution = hostSpecificMetadataConfiguration.incoming.resolutionResult.allComponents.map {
                (it as ResolvedComponentResult).variants.single().displayName
            }
        assertEquals(
            listOf("iosArm64CompilationDependenciesMetadata", "iosArm64MetadataElements-published", "iosArm64MetadataElements-published"),
            hostSpecificMetadataResolution
        )
    }
}
