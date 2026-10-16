package org.jetbrains.teamcity

import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.invocation.Gradle

object BuildFeatures {
    fun isConfigurationCacheRequested(gradle: Gradle): Boolean {
        return try {
            val buildFeatures = (gradle as org.gradle.api.internal.GradleInternal).services.get(BuildFeatures::class.java)
            buildFeatures.configurationCache.requested.getOrElse(false)
        } catch (e: Throwable) {
            try {
                @Suppress("DEPRECATION")
                gradle.startParameter.isConfigurationCacheRequested
            } catch (t: Throwable) {
                false
            }
        }
    }
}
