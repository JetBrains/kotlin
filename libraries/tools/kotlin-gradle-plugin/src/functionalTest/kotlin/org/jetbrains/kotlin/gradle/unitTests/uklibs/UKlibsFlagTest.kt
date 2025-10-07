package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.gradle.util.setUklibResolutionStrategy
import org.junit.Test
import kotlin.test.assertEquals

class UKlibsFlagTests {

    @Test
    fun test() {
        val kmpProjectDefault = buildProjectWithMPP(
            preApplyCode = {}
        ).evaluate()
        assertEquals(
            KmpPublicationStrategy.StandardKMPPublication,
            kmpProjectDefault.kotlinPropertiesProvider.kmpPublicationStrategy
        )
        assertEquals(
            KmpResolutionStrategy.StandardKMPResolution,
            kmpProjectDefault.kotlinPropertiesProvider.kmpResolutionStrategy
        )

        val kmpProjectWithUklibs = buildProjectWithMPP(
            preApplyCode = {
                propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_KMP_ENABLE_UKLIBS, "true")
            }
        ).evaluate()
        assertEquals(
            KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication,
            kmpProjectWithUklibs.kotlinPropertiesProvider.kmpPublicationStrategy
        )
        assertEquals(
            KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs,
            kmpProjectWithUklibs.kotlinPropertiesProvider.kmpResolutionStrategy
        )

        val kmpProjectWithStrategyOverride = buildProjectWithMPP(
            preApplyCode = {
                propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_KMP_ENABLE_UKLIBS, "true")
                setUklibResolutionStrategy(KmpResolutionStrategy.StandardKMPResolution)
            }
        ).evaluate()
        assertEquals(
            KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication,
            kmpProjectWithStrategyOverride.kotlinPropertiesProvider.kmpPublicationStrategy
        )
        assertEquals(
            KmpResolutionStrategy.StandardKMPResolution,
            kmpProjectWithStrategyOverride.kotlinPropertiesProvider.kmpResolutionStrategy
        )
    }

}